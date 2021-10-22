package cs451.interfaces;

import cs451.model.MessageModel;
import cs451.model.PacketModel;

/*
    It defines the methods to be implemented by a sender primitive.
    A sender primitive is loggable, thus a default implementation of log line has been provided.
 */
public interface SendInterface extends Loggable {
    // It sends the entire queue of messages and waits if it is empty
    void send();

    // Log line for broadcast message according to the README
    default String toLine(PacketModel packetModel) {
        return "b " + packetModel.getMessageId();
    }
}