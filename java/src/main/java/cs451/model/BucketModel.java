package cs451.model;

import cs451.Constants;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
    Defines a message that can be sent or has been received.
 */
public class MessageModel {

    // IPAddress and port from which a message has been received or to which a message must be sent.
    private AddressPortPair addressPortPair;

    private final int senderId;
    private int totalBytes;

    private final List<PacketModel> packetModelList;

    public MessageModel(AddressPortPair addressPortPair, int senderId, PacketModel packetModel) {
        this.addressPortPair = addressPortPair;
        this.senderId = senderId;

        this.totalBytes = Integer.BYTES;
        this.packetModelList = new ArrayList<>();

        addPacket(packetModel);
    }

    // Receiving a packet
    public MessageModel(byte[] messageBytes) {
        this.totalBytes = Integer.BYTES;
        this.packetModelList = new ArrayList<>();

        ByteBuffer bytes = ByteBuffer.wrap(messageBytes);

        this.senderId = bytes.getInt();
        int nextPacketSize = bytes.getInt();

        while(nextPacketSize > 0) {
            byte[] packetBytes = new byte[nextPacketSize];
            bytes.get(packetBytes, 0, nextPacketSize);

            this.packetModelList.add(new PacketModel(packetBytes, senderId));

            nextPacketSize = bytes.remaining() >= 4 ? bytes.getInt() : 0;
        }
    }

    public AddressPortPair getAddressPortPair() {
        return addressPortPair;
    }

    public InetAddress getIPAddress() {
        return addressPortPair.getIPAddress();
    }

    public int getPort() {
        return addressPortPair.getPort();
    }

    public int getSenderId() {
        return senderId;
    }

    public void addPacket(PacketModel packetModel) {
        this.packetModelList.add(packetModel);
        this.totalBytes += packetModel.getPacketSize() + Integer.BYTES;
    }

    public boolean isFull(PacketModel packetModel) {
        return this.totalBytes + packetModel.getPacketSize() + Integer.BYTES >= Constants.MAX_MESSAGE_LEN;
    }

    public List<PacketModel> getPacketModelList() {
        return packetModelList;
    }

    public int getTotalBytes() {
        return totalBytes;
    }

    public byte[] toBytes() {
        ByteBuffer bytes = ByteBuffer.allocate(this.totalBytes);

        bytes.putInt(senderId);

        for(PacketModel packetModel : packetModelList) {
            bytes.putInt(packetModel.getPacketSize());
            bytes.put(packetModel.toBytes());
        }

        return bytes.array();
    }


}
