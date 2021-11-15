package cs451.interfaces;

import cs451.model.PacketModel;

/*
    It defines the methods to be implemented by a deliver primitive.
    A deliver primitive is loggable, thus a default implementation of log line has been provided.
 */

public interface DeliverInterface<T> extends Loggable {
    //It delivers a single message
    void deliver(T packet);

    // Log line for delivered message according to the README
    default String toLine(PacketModel packetModel) {
        return "d " + packetModel.getSourceId() + " " + packetModel.getMessageId();
    }
}
