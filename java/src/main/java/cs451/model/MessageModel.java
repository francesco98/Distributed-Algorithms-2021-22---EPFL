package cs451.model;

import java.net.InetAddress;

/*
    Defines a message that can be sent or has been received.
 */
public class MessageModel implements Comparable<MessageModel> {

    // IPAddress and port from which a message has been received or to which a message must be sent.
    private final InetAddress IPAddress;
    private final int port;

    private final PacketModel packetModel;

    public MessageModel(InetAddress IPAddress, int port, PacketModel packetModel) {
        this.IPAddress = IPAddress;
        this.port = port;
        this.packetModel = packetModel;
    }

    public InetAddress getIPAddress() {
        return IPAddress;
    }

    public int getPort() {
        return port;
    }

    public PacketModel getPacket() {
        return packetModel;
    }

    @Override
    public int compareTo(MessageModel o) {
        return this.getPacket().getMessageId().compareTo(o.getPacket().getMessageId());
    }
}
