package cs451.interfaces;

import cs451.model.MessageModel;
import cs451.model.PacketModel;

/*
    A loggable class must implement the method toLine which the representation of a line in the log file.
 */

public interface Loggable {
    String toLine(PacketModel packetModel);
}
