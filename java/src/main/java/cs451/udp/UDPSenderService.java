package cs451.udp;

import cs451.Constants;
import cs451.interfaces.SenderListener;
import cs451.model.BucketModel;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
    It implements the UDP sender service to send a message over the channel.
    Different UDPSenderService can be sorted according to the message ID.
 */
public class UDPSenderService implements Closeable {
    private static int OPEN_SOCKET = 0;

    private static final Lock lock = new ReentrantLock();
    private static final Condition isAvailable = lock.newCondition();

    private final BucketModel messageModel;
    private final SenderListener senderListener;
    private final DatagramSocket datagramSocket;

    public static UDPSenderService getInstance(BucketModel messageModel, SenderListener senderListener) throws SocketException, InterruptedException {
        lock.lock();
        try {
            while (OPEN_SOCKET >= Constants.OPEN_SOCKET_LIMIT) {
                isAvailable.await();
            }
        } finally {
            lock.unlock();
        }

        return new UDPSenderService(messageModel, senderListener);
    }

    private UDPSenderService(BucketModel messageModel, SenderListener senderListener) throws SocketException {
        this.messageModel = messageModel;
        this.senderListener = senderListener;
        this.datagramSocket = new DatagramSocket();

        OPEN_SOCKET++;
    }

    public void prepareAndSend() {
        try {
            // Prepare the packet
            DatagramPacket datagramPacket = new DatagramPacket(
                    messageModel.toBytes(),
                    messageModel.getTotalBytes(),
                    messageModel.getIPAddress(),
                    messageModel.getPort());

            datagramSocket.send(datagramPacket);
            senderListener.onSent(this, messageModel);

        } catch (IOException exception) {
            // If the message is lost, it must be sent again.
            senderListener.onError(messageModel);
            this.close();
        }
    }

    public DatagramSocket getDatagramSocket() {
        return datagramSocket;
    }

    @Override
    public void close() {
        lock.lock();
        try {
            this.datagramSocket.close();
            OPEN_SOCKET--;

            isAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
