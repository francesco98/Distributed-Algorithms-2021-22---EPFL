package cs451.model;

/*
    It defines the composite key of a packet
 */
public class SenderMessageIDsPair {
    private final int sourceId;
    private final int messageId;

    public SenderMessageIDsPair(int sourceId, int messageId) {
        this.sourceId = sourceId;
        this.messageId = messageId;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getMessageId() {
        return messageId;
    }

    @Override
    public int hashCode() {
        // Credits: https://stackoverflow.com/questions/919612/mapping-two-integers-to-one-in-a-unique-and-deterministic-way
        // Cantor pairing function
        return (this.sourceId + this.messageId) * (this.sourceId + this.messageId + 1) / 2 + this.sourceId;
    }

    @Override
    public boolean equals(Object obj) {
        SenderMessageIDsPair senderMessageIDsPair = (SenderMessageIDsPair) obj;
        return this.sourceId == senderMessageIDsPair.getSourceId() &&
                this.messageId == senderMessageIDsPair.getMessageId();
    }
}
