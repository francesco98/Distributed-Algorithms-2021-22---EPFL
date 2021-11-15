package cs451.uniformreliable;

import cs451.Constants;
import cs451.Host;
import cs451.app.ProtocolType;
import cs451.besteffort.BebSend;
import cs451.fifo.FIFODeliver;
import cs451.interfaces.DeliverInterface;
import cs451.model.PacketModel;
import cs451.model.SenderMessageIDsPair;
import cs451.util.AbstractPrimitive;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class UrbDeliver extends AbstractPrimitive implements DeliverInterface<PacketModel> {
    private final ConcurrentHashMap<SenderMessageIDsPair, AtomicInteger> acks;
    private final BebSend bebSend;
    private final FIFODeliver fifoDeliver;
    private static ThreadPoolExecutor reBroadcastThread;

    private final int threshold;

    public UrbDeliver(Host host, List<Host> hosts, Set<String> log, ThreadPoolExecutor executorService) {
        super(host, hosts, log, executorService);

        this.acks = new ConcurrentHashMap<>();
        this.threshold = (hosts.size()/2) + 1;

        this.bebSend = new BebSend(host, hosts, log, executorService);
        this.fifoDeliver = new FIFODeliver(host, hosts, log, executorService);

        UrbDeliver.reBroadcastThread.execute(this.bebSend::send);
    }

    public static void setReBroadcastThread(ThreadPoolExecutor reBroadcastThread) {
        UrbDeliver.reBroadcastThread = reBroadcastThread;
    }

    @Override
    public synchronized void deliver(PacketModel receivedPacketModel) {

        if(!acks.containsKey(receivedPacketModel.getOriginalSenderMessageIDsPair())) {

            if(receivedPacketModel.getOriginalSourceId() != host.getId()) {
                receivedPacketModel.setSenderID(host.getId());
                this.bebSend.add(receivedPacketModel);
            }

            this.acks.put(receivedPacketModel.getOriginalSenderMessageIDsPair(), new AtomicInteger(1));
        } else {
            this.acks.get(receivedPacketModel.getOriginalSenderMessageIDsPair()).getAndIncrement();
        }

        if(this.acks.get(receivedPacketModel.getOriginalSenderMessageIDsPair()).get() == this.threshold) {
            if(Constants.PROTOCOL_TYPE == ProtocolType.UNIFORM_RELIABLE_BROADCAST) {
                System.out.println("Packet: " + receivedPacketModel.getMessageId() + " from host: " + receivedPacketModel.getOriginalSourceId() + " delivered");
                log(toLine(receivedPacketModel));
            } else {
                this.fifoDeliver.deliver(receivedPacketModel);
            }

        }

    }

    @Override
    public String toLine(PacketModel packetModel) {
        return "d " + packetModel.getOriginalSourceId() + " " + packetModel.getMessageId();
    }
}
