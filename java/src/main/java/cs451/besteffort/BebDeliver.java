package cs451.besteffort;

import cs451.Constants;
import cs451.Host;
import cs451.app.ProtocolType;
import cs451.interfaces.DeliverInterface;
import cs451.model.PacketModel;
import cs451.uniformreliable.UrbDeliver;
import cs451.util.AbstractPrimitive;

import java.net.DatagramPacket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class BebDeliver extends AbstractPrimitive implements DeliverInterface<PacketModel> {
    private final UrbDeliver urbDeliver;

    public BebDeliver(Host host, List<Host> hosts, Set<String> log, ThreadPoolExecutor executorService) {
        super(host, hosts, log, executorService);
        this.urbDeliver = new UrbDeliver(host, hosts, log, executorService);
    }

    @Override
    public void deliver(PacketModel receivedPacketModel) {
        if(Constants.PROTOCOL_TYPE == ProtocolType.BEST_EFFORT_BROADCAST) {
            log(toLine(receivedPacketModel));
        } else {
            urbDeliver.deliver(receivedPacketModel);
        }

        //final PacketModel receivedPacketModel = new PacketModel(datagramPacket.getData());
        //log(toLine(receivedPacketModel));

        /* if(log(toLine(receivedPacketModel))) {
            System.out.println("Packet: " + receivedPacketModel.getMessageId() + " from host: " + receivedPacketModel.getSourceId() + " delivered");
         } */

    }
}
