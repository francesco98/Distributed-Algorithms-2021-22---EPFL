package cs451.uniformreliable;

import cs451.Constants;
import cs451.Host;
import cs451.app.ProtocolType;
import cs451.besteffort.BebSend;
import cs451.fifo.FIFODeliver;
import cs451.interfaces.DeliverInterface;
import cs451.interfaces.Writer;
import cs451.model.PacketModel;
import cs451.model.SenderMessageIDsPair;
import cs451.util.AbstractPrimitive;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class UrbDeliver extends AbstractPrimitive implements DeliverInterface<PacketModel> {
    //private static ThreadPoolExecutor reBroadcastThread;
    private static ThreadPoolExecutor addMessagesThread;
    private static BebSend bebSend;

    private final ConcurrentHashMap<SenderMessageIDsPair, AtomicInteger> acks;
    //private final BebSend bebSend;
    private final FIFODeliver fifoDeliver;

    private final int threshold;

    public UrbDeliver(Host host, List<Host> hosts, Set<String> log, Writer writer, ThreadPoolExecutor executorService) {
        super(host, hosts, log, writer, executorService);

        this.acks = new ConcurrentHashMap<>();
        this.threshold = (hosts.size()/2) + 1;

        //this.bebSend = new BebSend(host, hosts, log, executorService);
        this.fifoDeliver = new FIFODeliver(host, hosts, log, writer, executorService);

        //UrbDeliver.reBroadcastThread.execute(this.bebSend::send);
    }

    public static void setReBroadcastThreads(/*ThreadPoolExecutor reBroadcastThread, */ThreadPoolExecutor addMessagesThread) {
        //UrbDeliver.reBroadcastThread = reBroadcastThread;
        UrbDeliver.addMessagesThread = addMessagesThread;
    }

    public static void setBebSend(BebSend bebSend) {
        UrbDeliver.bebSend = bebSend;
    }

    @Override
    public synchronized void deliver(PacketModel receivedPacketModel) {

        if(!acks.containsKey(receivedPacketModel.getOriginalSenderMessageIDsPair())) {

            if(receivedPacketModel.getOriginalSourceId() != host.getId()) {
                receivedPacketModel.setSenderID(host.getId());
                UrbDeliver.addMessagesThread.execute(() -> UrbDeliver.bebSend.blockingAdd(receivedPacketModel));
                //UrbDeliver.bebSend.add(receivedPacketModel);
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
