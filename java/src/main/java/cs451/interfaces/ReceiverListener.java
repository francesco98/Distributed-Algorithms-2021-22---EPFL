package cs451.interfaces;

import cs451.model.MessageModel;

import java.net.DatagramPacket;

/*
    It is used as listener when an UDP packet has been received.
 */
public interface ReceiverListener {
    void onReceive(DatagramPacket datagramPacket);
}
