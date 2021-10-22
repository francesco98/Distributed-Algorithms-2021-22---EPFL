package cs451.udp;

import cs451.Constants;
import cs451.interfaces.ReceiverListener;
import cs451.model.MessageModel;
import cs451.model.PacketModel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/*
    It implements the UDP receiver calling a listener whenever a packet has been received.
 */
public class UDPReceiverService implements Runnable {

    private final DatagramSocket datagramSocket;
    private final ReceiverListener receiverListener;

    public UDPReceiverService(int port, ReceiverListener receiverListener) throws SocketException {
        this.datagramSocket = new DatagramSocket(port);
        this.receiverListener = receiverListener;
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            try {
                byte[] receivedBytes = new byte[Constants.MAX_PACKET_LEN];
                DatagramPacket datagramPacket = new DatagramPacket(receivedBytes, Constants.MAX_PACKET_LEN);
                datagramSocket.receive(datagramPacket);




                // Notify the event (to deliver it)
                receiverListener.onReceive(datagramPacket);

            } catch (IOException exception) {
                System.out.println("UDP receiver has been stopped");
            }
        }

    }
}
