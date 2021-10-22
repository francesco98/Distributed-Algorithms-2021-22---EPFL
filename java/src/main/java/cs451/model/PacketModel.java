package cs451.model;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/*
    Defines the structure of a packet and all the methods to convert it into bytes or making it from bytes.
 */
public class PacketModel implements Serializable {
    // It is the ID of the packet
    private final SenderMessageIDsPair senderMessageIDsPair;
    private String payload;

    // It is NOT included in the packet. It is used just to track the id of the receiving host.
    private int destinationId;

    // Packet with payload
    public PacketModel(int sourceId, int destinationId, int messageId, String payload) {
        this.senderMessageIDsPair = new SenderMessageIDsPair(sourceId, messageId);

        this.destinationId = destinationId;
        this.payload = payload;
    }

    // Packet for acknowledgment
    public PacketModel(int sourceId, int messageId) {
        this.senderMessageIDsPair = new SenderMessageIDsPair(sourceId, messageId);
    }

    // Receiving a packet
    public PacketModel(byte[] packetBytes) {
        ByteBuffer bytes = ByteBuffer.wrap(packetBytes);

        this.senderMessageIDsPair = new SenderMessageIDsPair(bytes.getInt(), bytes.getInt());
        this.payload = new String(bytes.array(), StandardCharsets.UTF_8);
    }

    public int getSourceId() {
        return this.senderMessageIDsPair.getSourceId();
    }

    public int getDestinationId() {
        return destinationId;
    }

    public Integer getMessageId() {
        return this.senderMessageIDsPair.getMessageId();
    }

    public String getPayload() {
        return payload;
    }

    public static int getHeaderSize() {
        return Integer.SIZE * 2;
    }

    public int getPacketSize() {
        return getHeaderSize() + (getPayload() != null ? getPayload().getBytes().length : 0);
    }

    public byte[] toBytes() {
        ByteBuffer bytes = ByteBuffer.allocate(getPacketSize());
        bytes.putInt(getSourceId());
        bytes.putInt(getMessageId());

        if(payload != null)
            bytes.put(payload.getBytes());

        return bytes.array();
    }
}
