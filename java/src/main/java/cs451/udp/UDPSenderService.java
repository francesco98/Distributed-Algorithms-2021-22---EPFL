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

    private final BucketModel bucketModel;
    private final SenderListener senderListener;
    private final DatagramSocket datagramSocket;

    public static UDPSenderService getInstance(BucketModel bucketModel, SenderListener senderListener) throws SocketException, InterruptedException {
        lock.lock();
        try {
            while (OPEN_SOCKET >= Constants.OPEN_SOCKET_LIMIT) {
                // System.out.println("Blocked (2)");
                isAvailable.await();
            }
        } finally {
            lock.unlock();
        }

        return new UDPSenderService(bucketModel, senderListener);
    }

    private UDPSenderService(BucketModel bucketModel, SenderListener senderListener) throws SocketException {
        this.bucketModel = bucketModel;
        this.senderListener = senderListener;
        this.datagramSocket = new DatagramSocket();

        OPEN_SOCKET++;
    }

    public void prepareAndSend() {
        try {
            // Prepare the packet
            DatagramPacket datagramPacket = new DatagramPacket(
                    this.bucketModel.toBytes(),
                    this.bucketModel.getTotalBytes(),
                    this.bucketModel.getIPAddress(),
                    this.bucketModel.getPort());

            senderListener.onSending(this.bucketModel);
            datagramSocket.send(datagramPacket);
            senderListener.onSent(this, this.bucketModel);

        } catch (IOException exception) {
            // If the message is lost, it must be sent again.
            senderListener.onError(bucketModel);
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

            isAvailable.signal();
        } finally {
            lock.unlock();
        }
    }
}
