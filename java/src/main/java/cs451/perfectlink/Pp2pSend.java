package cs451.perfectlink;

import cs451.Constants;
import cs451.Host;
import cs451.interfaces.SendInterface;
import cs451.interfaces.SenderListener;
import cs451.model.MessageModel;
import cs451.model.PacketModel;
import cs451.udp.UDPSenderService;
import cs451.util.AbstractPrimitive;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.*;

/*
    It defines the Pp2pSend primitive.
    It extends the AbstractPrimitive to log send events and to spawn threads.
 */
public class Pp2pSend extends AbstractPrimitive implements SendInterface {
    // The queue of messages to be sent
    private final BlockingDeque<MessageModel> messagesQueue;

    public Pp2pSend(Host host, Set<String> log, ThreadPoolExecutor executorService) {
        super(host, log, executorService);
        this.messagesQueue = new LinkedBlockingDeque<>();
    }


    public void add(final MessageModel messageModel) {
        this.messagesQueue.add(messageModel);
    }

    public void waitForAck(UDPSenderService service, MessageModel messageModel) {
        try (service) {
            // Wait for the acknowledgment packet
            byte[] receivedBytes = ByteBuffer.allocate(PacketModel.getHeaderSize()).array();
            DatagramPacket receivedPacket = new DatagramPacket(receivedBytes, PacketModel.getHeaderSize());

            // Maximum timeout
            service.getDatagramSocket().setSoTimeout(Constants.ACK_TIMEOUT_MS);
            service.getDatagramSocket().receive(receivedPacket);
            PacketModel packetModel = new PacketModel(receivedBytes);

            // If the ack is not correct, the message must be sent again
            if (messageModel.getPacket().getDestinationId() != packetModel.getSourceId() ||
                    !messageModel.getPacket().getMessageId().equals(packetModel.getMessageId())) {
                messagesQueue.addFirst(messageModel);
            } else {
                System.out.println("Ack received from " + packetModel.getSourceId() + " for message: " + messageModel.getPacket().getMessageId());
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Ack timeout for: " + messageModel.getPacket().getMessageId());
        } catch (IOException e) {
            messagesQueue.addFirst(messageModel);
        }
    }

    @Override
    public void send() {
        while (!Thread.interrupted()) {
            try {
                // Wait until a the queue is non-empty and poll the next message to send.
                MessageModel messageModel = this.messagesQueue.take();

                UDPSenderService.getInstance(messageModel, new SenderListener() {
                    @Override
                    public void onSent(UDPSenderService service, MessageModel messageModel) {
                        // Ack is async
                        start(() -> waitForAck(service, messageModel));

                        log(toLine(messageModel.getPacket()));
                    }

                    @Override
                    public void onError(MessageModel messageModel) {
                        // If the message has not sent successfully, add it to the top of the queue.
                        messagesQueue.addFirst(messageModel);
                    }
                }).prepareAndSend();

            } catch (InterruptedException | SocketException e) {
                System.out.println("Sender thread has been stopped");
            }
        }
    }
}
