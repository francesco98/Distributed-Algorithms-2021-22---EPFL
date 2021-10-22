package cs451.perfectlink;

import cs451.Host;
import cs451.interfaces.DeliverInterface;
import cs451.model.MessageModel;
import cs451.model.PacketModel;
import cs451.model.SenderMessageIDsPair;
import cs451.util.AbstractPrimitive;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/*
    It defines the Pp2pDeliver primitive.
    It extends the AbstractPrimitive to log deliver events and to spawn threads.
 */
public class Pp2pDeliver extends AbstractPrimitive implements DeliverInterface {

    public Pp2pDeliver(Host host, Set<String> log, ThreadPoolExecutor executorService) {
        super(host, log, executorService);
    }

    @Override
    public void deliver(final DatagramPacket datagramPacket) {
        start(() -> {
            try(DatagramSocket datagramSocket = new DatagramSocket()) {

                // Build packet from bytes
                final PacketModel receivedPacketModel = new PacketModel(datagramPacket.getData());

                // The acknowledgment packet is just the senderId who received the packet and the id of the message
                PacketModel packetModel = new PacketModel(
                        host.getId(),
                        receivedPacketModel.getMessageId());

                // The UDP ack packet is sent to the same address and port on which the packet is received
                DatagramPacket ackPacket = new DatagramPacket(
                        packetModel.toBytes(),
                        packetModel.getPacketSize(),
                        datagramPacket.getAddress(),
                        datagramPacket.getPort());

                datagramSocket.send(ackPacket);

                /*
                    The use of another data structure can lead to "OutOfMemoryError"
                    NOTE: To log the delivered message a set is used, so no other structures are needed!!!
                 */

                //log(toLine(messageModel));

                if(log(toLine(receivedPacketModel))) {
                    System.out.println("Packet: " + receivedPacketModel.getMessageId() + " from host: " + receivedPacketModel.getSourceId() + " delivered");
                }


            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });
    }
}
