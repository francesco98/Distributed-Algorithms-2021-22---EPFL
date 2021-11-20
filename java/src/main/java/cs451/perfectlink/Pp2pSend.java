package cs451.perfectlink;

import cs451.Constants;
import cs451.Host;
import cs451.interfaces.SendInterface;
import cs451.interfaces.SenderListener;
import cs451.interfaces.Writer;
import cs451.model.AddressPortPair;
import cs451.model.BucketModel;
import cs451.model.PacketModel;
import cs451.udp.UDPSenderService;
import cs451.util.AbstractPrimitive;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
    It defines the Pp2pSend primitive.
    It extends the AbstractPrimitive to log send events and to spawn threads.
 */
public class Pp2pSend extends AbstractPrimitive implements SendInterface {
    // The queue of messages to be sent
    private final ConcurrentHashMap<AddressPortPair, LinkedBlockingDeque<BucketModel>> bucketsQueue;

    private final ConcurrentHashMap<AddressPortPair, AtomicInteger> totalMessagesToSent;
    private final ConcurrentHashMap<AddressPortPair, AtomicInteger> myMessagesToSent;

    private final Set<Integer> broadcastMessages;

    private final Lock lock = new ReentrantLock();

    private final Condition totalCondition = lock.newCondition();
    private final Condition ownCondition = lock.newCondition();

    public Pp2pSend(Host host, List<Host> hosts, Set<String> log, Writer writer, ThreadPoolExecutor executorService) {
        super(host, hosts, log, writer, executorService);
        this.bucketsQueue = new ConcurrentHashMap<>();
        this.totalMessagesToSent = new ConcurrentHashMap<>();
        this.myMessagesToSent = new ConcurrentHashMap<>();

        this.broadcastMessages = Collections.synchronizedSet(new LinkedHashSet<>());

        for (Host h : hosts) {
            try {
                InetAddress inetAddress = InetAddress.getByName(h.getIp());
                AddressPortPair addressPortPair = new AddressPortPair(inetAddress, h.getPort());

                bucketsQueue.put(addressPortPair, new LinkedBlockingDeque<>());

                totalMessagesToSent.put(addressPortPair, new AtomicInteger(0));
                myMessagesToSent.put(addressPortPair, new AtomicInteger(0));

            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    private void addBucketFirst(final BucketModel bucketModel) {
        this.bucketsQueue.get(bucketModel.getAddressPortPair()).addFirst(bucketModel);
    }

    private void addBucket(final BucketModel bucketModel) {
        this.bucketsQueue.get(bucketModel.getAddressPortPair()).addLast(bucketModel);
    }

    private boolean isQueueFree(boolean isOwn) {
        int nFreeQueue = 0;
        for (AtomicInteger queueSize : (isOwn ? this.myMessagesToSent.values() : this.totalMessagesToSent.values())) {
            if (queueSize.get() < Constants.MAX_QUEUE_SIZE / (isOwn ? this.hosts.size() : 1))
                nFreeQueue++;
        }

        return nFreeQueue > (this.hosts.size() / 2);
    }

    public void blockingAdd(final AddressPortPair addressPortPair, final PacketModel packetModel) {
        lock.lock();

        try {
            if (!isQueueFree(false)) {
                totalCondition.await();
            }
            if (packetModel.isOwn() && !isQueueFree(true)) {
                ownCondition.await();
            }
            this.add(addressPortPair, packetModel);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

    }

    private void add(final AddressPortPair addressPortPair, final PacketModel packetModel) {
        try {
            BucketModel bucketModel = this.bucketsQueue.get(addressPortPair).pollLast();
            if (bucketModel != null) {
                if (bucketModel.isFull(packetModel)) {
                    addBucket(bucketModel);
                    addBucket(new BucketModel(addressPortPair, host.getId(), packetModel));
                } else {
                    bucketModel.addPacket(packetModel);
                    bucketsQueue.get(addressPortPair).addLast(bucketModel);
                }
            } else {
                addBucket(new BucketModel(addressPortPair, host.getId(), packetModel));
            }

            this.totalMessagesToSent.get(addressPortPair).incrementAndGet();
            if (packetModel.isOwn()) {
                this.myMessagesToSent.get(addressPortPair).incrementAndGet();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void waitForAck(UDPSenderService service, BucketModel bucketModel) {
        try (service) {
            // Wait for the acknowledgment packet
            byte[] receivedBytes = ByteBuffer.allocate(PacketModel.getAckSize()).array();
            DatagramPacket receivedPacket = new DatagramPacket(receivedBytes, PacketModel.getAckSize());

            // Maximum timeout
            service.getDatagramSocket().setSoTimeout(Constants.ACK_TIMEOUT_MS);
            service.getDatagramSocket().receive(receivedPacket);
            PacketModel packetModel = new PacketModel(receivedBytes);

            // If the ack is not correct, the message must be sent again
            if (bucketModel.getFirst().getDestinationId() != packetModel.getSourceId() ||
                    bucketModel.getFirst().getOriginalSourceId() != packetModel.getOriginalSourceId() ||
                    !bucketModel.getFirst().getMessageId().equals(packetModel.getMessageId())) {
                this.addBucketFirst(bucketModel);
            } else {
                this.myMessagesToSent.get(bucketModel.getAddressPortPair()).addAndGet(-bucketModel.getOwnPackets().size());
                this.totalMessagesToSent.get(bucketModel.getAddressPortPair()).addAndGet(-bucketModel.getPacketModelList().size());

                lock.lock();
                try {
                    if (isQueueFree(false)) {
                        totalCondition.signal();
                    }
                    if (isQueueFree(true)) {
                        ownCondition.signal();
                    }
                } finally {
                    lock.unlock();
                }

                // System.out.println("Ack received from " + packetModel.getSourceId() + " for message: " + bucketModel.getFirst().getMessageId() + " original sent by: " + bucketModel.getFirst().getOriginalSourceId());
            }
        } catch (Exception e) {
            System.out.println("Ack timeout for: " + bucketModel.getFirst().getMessageId() + " to: " + bucketModel.getAddressPortPair().getPort());
            this.addBucketFirst(bucketModel);
        }
    }

    @Override
    public void send() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            System.out.println("Sender thread cannot start");
        }

        while (!Thread.interrupted()) {
            this.bucketsQueue.forEach((addressPortPair, queue) -> {
                try {
                    if (!queue.isEmpty()) {
                        // Wait until a the queue is non-empty and poll the next message to send.
                        BucketModel messageModel = this.bucketsQueue.get(addressPortPair).pollFirst();

                        if (messageModel != null) {
                            UDPSenderService.getInstance(messageModel, new SenderListener() {
                                @Override
                                public void onSending(BucketModel bucketModel) {
                                    synchronized (Pp2pSend.this) {
                                        for (PacketModel packetModel : bucketModel.getPacketModelList()) {
                                            if (packetModel.getOriginalSourceId() == packetModel.getSourceId() && Pp2pSend.this.broadcastMessages.add(packetModel.getMessageId())) {
                                                log(toLine(packetModel));
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onSent(UDPSenderService service, BucketModel bucketSent) {
                                    // Ack is async
                                    start(() -> waitForAck(service, bucketSent));
                                }

                                @Override
                                public void onError(BucketModel bucketError) {
                                    // If the message has not sent successfully, add it to the top of the queue.
                                    Pp2pSend.this.addBucketFirst(bucketError);
                                }
                            }).prepareAndSend();
                        }

                        Thread.sleep(10);
                    }
                } catch (InterruptedException | SocketException e) {
                    System.out.println("Sender thread has been stopped");
                }
            });

        }
    }
}
