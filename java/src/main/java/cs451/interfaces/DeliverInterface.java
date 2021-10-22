package cs451.interfaces;

import cs451.model.MessageModel;
import cs451.model.PacketModel;

import java.net.DatagramPacket;

/*
    It defines the methods to be implemented by a deliver primitive.
    A deliver primitive is loggable, thus a default implementation of log line has been provided.
 */

public interface DeliverInterface extends Loggable {
    //It delivers a single message
    void deliver(DatagramPacket datagramPacket);

    // Log line for delivered message according to the README
    default String toLine(PacketModel packetModel) {
        return "d " + packetModel.getSourceId() + " " + packetModel.getMessageId();
    }
}
