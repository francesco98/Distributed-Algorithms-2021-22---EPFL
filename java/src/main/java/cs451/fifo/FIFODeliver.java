package cs451.fifo;

import cs451.Host;
import cs451.interfaces.DeliverInterface;
import cs451.model.PacketModel;
import cs451.model.SenderMessageIDsPair;
import cs451.util.AbstractPrimitive;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class FIFODeliver extends AbstractPrimitive implements DeliverInterface<PacketModel> {
    private final ConcurrentHashMap<Integer, PriorityBlockingQueue<PacketModel>> messagesToDeliver;
    private final ConcurrentHashMap<Integer, AtomicInteger> lastDeliveredPacket;

    public FIFODeliver(Host host, List<Host> hosts, Set<String> log, ThreadPoolExecutor executorService) {
        super(host, hosts, log, executorService);

        this.messagesToDeliver = new ConcurrentHashMap<>();
        this.lastDeliveredPacket = new ConcurrentHashMap<>();

        for (Host h : hosts) {
            this.lastDeliveredPacket.put(h.getId(), new AtomicInteger(0));
            this.messagesToDeliver.put(h.getId(), new PriorityBlockingQueue<>());
        }
    }

    @Override
    public synchronized void deliver(PacketModel packet) {
        int lastMessage = this.lastDeliveredPacket.get(packet.getOriginalSourceId()).get();

        if (packet.getMessageId() > lastMessage) {
            if (packet.getMessageId() == lastMessage + 1) {

                log(toLine(packet));
                System.out.println("Packet: " + packet.getMessageId() + " from host: " + packet.getOriginalSourceId() + " delivered");

                int currentMessageId = this.lastDeliveredPacket.get(packet.getOriginalSourceId()).incrementAndGet();
                PacketModel nextMessage = messagesToDeliver.get(packet.getOriginalSourceId()).poll();

                while (nextMessage != null && nextMessage.getMessageId() == currentMessageId + 1) {
                    log(toLine(nextMessage));
                    System.out.println("Packet: " + nextMessage.getMessageId() + " from host: " + nextMessage.getOriginalSourceId() + " delivered");

                    currentMessageId = this.lastDeliveredPacket.get(packet.getOriginalSourceId()).incrementAndGet();
                    nextMessage = this.messagesToDeliver.get(packet.getOriginalSourceId()).poll();
                }

                if (nextMessage != null) {
                    this.messagesToDeliver.get(packet.getOriginalSourceId()).put(nextMessage);
                }
            } else {
                this.messagesToDeliver.get(packet.getOriginalSourceId()).put(packet);
            }
        }
    }

    @Override
    public String toLine(PacketModel packetModel) {
        return "d " + packetModel.getOriginalSourceId() + " " + packetModel.getMessageId();
    }
}
