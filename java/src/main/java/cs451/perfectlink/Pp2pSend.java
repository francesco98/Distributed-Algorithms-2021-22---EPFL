package cs451.perfectlink;

import cs451.Constants;
import cs451.Host;
import cs451.interfaces.SendInterface;
import cs451.interfaces.SenderListener;
import cs451.model.AddressPortPair;
import cs451.model.BucketModel;
import cs451.model.PacketModel;
import cs451.udp.UDPSenderService;
import cs451.util.AbstractPrimitive;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntUnaryOperator;

/*
    It defines the Pp2pSend primitive.
    It extends the AbstractPrimitive to log send events and to spawn threads.
 */
public class Pp2pSend extends AbstractPrimitive implements SendInterface {
    // The queue of messages to be sent
    private final ConcurrentHashMap<AddressPortPair, LinkedBlockingDeque<BucketModel>> bucketsQueue;

    private int lastMessageAdded;
    private int lastMessageSent;

    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public Pp2pSend(Host host, List<Host> hosts, Set<String> log, ThreadPoolExecutor executorService) {
        super(host, hosts, log, executorService);

        this.bucketsQueue = new ConcurrentHashMap<>();

        this.lastMessageSent = 0;
        this.lastMessageAdded = 0;
    }

    private void addBucket(final BucketModel bucketModel) {
        this.bucketsQueue.get(bucketModel.getAddressPortPair()).addLast(bucketModel);
    }

    public void blockingAdd(final AddressPortPair addressPortPair, final PacketModel packetModel) {
        lock.lock();

        try {
            this.lastMessageAdded = packetModel.getMessageId();

            if (this.lastMessageAdded - this.lastMessageSent > Constants.MAX_QUEUE_PACKETS) {
                condition.await();
            }
            this.add(addressPortPair, packetModel);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

    }

    public void add(final AddressPortPair addressPortPair, final PacketModel packetModel) {
        bucketsQueue.putIfAbsent(addressPortPair, new LinkedBlockingDeque<>());

        try {
            BucketModel bucketModel = this.bucketsQueue.get(addressPortPair).pollLast();
            if (bucketModel != null) {
                if (bucketModel.isFull(packetModel)) {
                    addBucket(bucketModel);
                    addBucket(new BucketModel(addressPortPair, host.getId(), packetModel));
                } else {
                    bucketModel.addPacket(packetModel);
                    bucketsQueue.get(addressPortPair).add(bucketModel);
                }
            } else {
                addBucket(new BucketModel(addressPortPair, host.getId(), packetModel));
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
                this.addBucket(bucketModel);
            } else {
                // System.out.println("Ack received from " + packetModel.getSourceId() + " for message: " + bucketModel.getFirst().getMessageId() + " original sent by: " + bucketModel.getFirst().getOriginalSourceId());
                PacketModel lastPacket = bucketModel.getLast();

                if (lastPacket.getSourceId() == lastPacket.getOriginalSourceId() && lastPacket.getMessageId() > this.lastMessageSent) {
                    lock.lock();
                    try {
                        this.lastMessageSent = lastPacket.getMessageId();
                        if (this.lastMessageAdded - this.lastMessageSent <= Constants.MAX_QUEUE_PACKETS) {
                            condition.signal();
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            }
        } catch (Exception e) {
            //System.out.println("Ack timeout for: " + bucketModel.getPacket().getMessageId());
            this.addBucket(bucketModel);
        }
    }

    @Override
    public void send() {
        while (!Thread.interrupted()) {
            this.bucketsQueue.forEach((addressPortPair, queue) -> {
                try {
                    if (!queue.isEmpty()) {
                        // Wait until a the queue is non-empty and poll the next message to send.
                        BucketModel messageModel = this.bucketsQueue.get(addressPortPair).pollFirst();

                        if (messageModel != null) {
                            UDPSenderService.getInstance(messageModel, new SenderListener() {
                                @Override
                                public void onSent(UDPSenderService service, BucketModel messageModel) {
                                    // Ack is async
                                    start(() -> waitForAck(service, messageModel));

                                    if (messageModel.getFirst().getOriginalSourceId() == messageModel.getFirst().getSourceId()) {
                                        for (PacketModel packetModel : messageModel.getPacketModelList())
                                            log(toLine(packetModel));
                                    }

                                }

                                @Override
                                public void onError(BucketModel messageModel) {
                                    // If the message has not sent successfully, add it to the top of the queue.
                                    Pp2pSend.this.addBucket(messageModel);
                                }
                            }).prepareAndSend();
                        }
                    }
                } catch (InterruptedException | SocketException e) {
                    System.out.println("Sender thread has been stopped");
                }
            });

        }
    }
}
