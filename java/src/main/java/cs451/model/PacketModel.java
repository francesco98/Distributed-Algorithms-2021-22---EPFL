package cs451.model;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/*
    Defines the structure of a packet and all the methods to convert it into bytes or making it from bytes.
 */
public class PacketModel implements Serializable, Comparable<PacketModel> {
    private final SenderMessageIDsPair originalSenderMessageIDsPair;
    private SenderMessageIDsPair senderMessageIDsPair;

    private String payload;

    // It is NOT included in the packet. It is used just to track the id of the receiving host.
    private int destinationId;

    // Packet with payload
    public PacketModel(int sourceId, int messageId, String payload) {
        this.originalSenderMessageIDsPair = new SenderMessageIDsPair(sourceId, messageId);
        this.senderMessageIDsPair = new SenderMessageIDsPair(sourceId, messageId);
        this.payload = payload;
    }

    public void setDestinationId(int destinationId) {
        this.destinationId = destinationId;
    }

    // Packet for acknowledgment
    public PacketModel(int sourceId, int originalSourceId, int messageId) {
        this.originalSenderMessageIDsPair = new SenderMessageIDsPair(originalSourceId, messageId);
        this.senderMessageIDsPair = new SenderMessageIDsPair(sourceId, messageId);
    }

    public void setSenderID(int senderId) {
        this.senderMessageIDsPair = new SenderMessageIDsPair(senderId, getMessageId());
    }

    // Receiving an ACK packet (sourceId is known)
    public PacketModel(byte[] packetBytes) {
        this(Arrays.copyOfRange(packetBytes, Integer.BYTES, packetBytes.length), ByteBuffer.wrap(packetBytes).getInt());
    }

    // Receiving a packet
    public PacketModel(byte[] packetBytes, int sourceId) {
        ByteBuffer bytes = ByteBuffer.wrap(packetBytes);

        int originalSourceId = bytes.getInt();
        int messageId = bytes.getInt();

        this.originalSenderMessageIDsPair = new SenderMessageIDsPair(originalSourceId, messageId);
        this.senderMessageIDsPair = new SenderMessageIDsPair(sourceId, messageId);

        this.payload = new String(bytes.array(), StandardCharsets.UTF_8);
    }

    public SenderMessageIDsPair getOriginalSenderMessageIDsPair() {
        return originalSenderMessageIDsPair;
    }

    public int getOriginalSourceId() {
        return this.originalSenderMessageIDsPair.getSourceId();
    }

    public int getSourceId() { return this.senderMessageIDsPair.getSourceId(); }

    public int getDestinationId() {
        return destinationId;
    }

    public Integer getMessageId() {
        return this.originalSenderMessageIDsPair.getMessageId();
    }

    public String getPayload() {
        return payload;
    }

    public static int getHeaderSize() {
        return Integer.BYTES * 2;
    }

    public static int getAckSize() {
        return Integer.BYTES * 3;
    }

    public int getPacketSize() {
        return getHeaderSize() + (getPayload() != null ? getPayload().getBytes().length : 0);
    }

    public byte[] toBytes() {
        ByteBuffer bytes = ByteBuffer.allocate(getPacketSize());
        bytes.putInt(getOriginalSourceId());
        bytes.putInt(getMessageId());

        if(payload != null)
            bytes.put(payload.getBytes());

        return bytes.array();
    }

    public byte[] toAckBytes() {
        ByteBuffer bytes = ByteBuffer.allocate(getAckSize());
        bytes.putInt((getSourceId()));
        bytes.putInt(getOriginalSourceId());
        bytes.putInt(getMessageId());

        if(payload != null)
            bytes.put(payload.getBytes());

        return bytes.array();
    }

    public Long getId() {
        return Long.parseLong(String.valueOf(getSourceId()) + getOriginalSourceId() + getMessageId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSourceId(), getOriginalSourceId(), getMessageId());
    }

    @Override
    public boolean equals(Object obj) {
        PacketModel pm = (PacketModel) obj;
        return this.getSourceId() == pm.getSourceId() &&
                this.getOriginalSourceId() == pm.getOriginalSourceId() &&
                this.getMessageId().equals(pm.getMessageId());
    }

    @Override
    public String toString() {
        return "(" + this.getSourceId() + "," + this.getOriginalSourceId() + "," + this.getMessageId() + ")";
    }

    @Override
    public int compareTo(PacketModel o) {
        return this.getMessageId().compareTo(o.getMessageId());
    }
}
