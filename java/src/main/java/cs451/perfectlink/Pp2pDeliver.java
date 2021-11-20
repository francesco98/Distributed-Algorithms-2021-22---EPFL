package cs451.perfectlink;

import cs451.Constants;
import cs451.Host;
import cs451.app.ProtocolType;
import cs451.besteffort.BebDeliver;
import cs451.interfaces.DeliverInterface;
import cs451.interfaces.Writer;
import cs451.model.BucketModel;
import cs451.model.PacketModel;
import cs451.util.AbstractPrimitive;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

/*
    It defines the Pp2pDeliver primitive.
    It extends the AbstractPrimitive to log deliver events and to spawn threads.
 */
public class Pp2pDeliver extends AbstractPrimitive implements DeliverInterface<DatagramPacket> {
    private final BebDeliver bebDeliver;
    private final Set<Long> delivered;

    public Pp2pDeliver(Host host, List<Host> hosts, Set<String> log, Writer writer, ThreadPoolExecutor executorService) {
        super(host, hosts, log, writer, executorService);
        this.bebDeliver = new BebDeliver(host, hosts, log, writer, executorService);
        this.delivered = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public void deliver(final DatagramPacket datagramPacket) {
        start(() -> {
            try (DatagramSocket datagramSocket = new DatagramSocket()) {

                // Build packet from bytes
                final BucketModel receivedBucketModel = new BucketModel(datagramPacket.getData());

                // The acknowledgment packet is just the senderId who received the packet and the id of the message
                PacketModel packetModel = new PacketModel(
                        host.getId(),
                        receivedBucketModel.getFirst().getOriginalSourceId(),
                        receivedBucketModel.getFirst().getMessageId());

                // The UDP ack packet is sent to the same address and port on which the packet is received
                DatagramPacket ackPacket = new DatagramPacket(
                        packetModel.toAckBytes(),
                        PacketModel.getAckSize(),
                        datagramPacket.getAddress(),
                        datagramPacket.getPort());

                // System.out.println("SENDING ACK FOR: " + receivedBucketModel.getFirst().getMessageId() + " TO: " + receivedBucketModel.getFirst().getSourceId());

                datagramSocket.send(ackPacket);

                for (PacketModel receivedPacketModel : receivedBucketModel.getPacketModelList()) {
                    if (Constants.PROTOCOL_TYPE == ProtocolType.PERFECT_LINKS) {
                        log(toLine(receivedPacketModel));
                    } else {
                        if (delivered.add(receivedPacketModel.getId())) {
                            // System.out.println(receivedPacketModel.getOriginalSourceId() + " " + receivedPacketModel.getSourceId() + " " + receivedPacketModel.getMessageId());
                            bebDeliver.deliver(receivedPacketModel);
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
