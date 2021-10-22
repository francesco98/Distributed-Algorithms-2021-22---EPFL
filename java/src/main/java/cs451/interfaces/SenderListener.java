package cs451.interfaces;

import cs451.model.MessageModel;
import cs451.udp.UDPSenderService;

import java.net.DatagramSocket;

/*
    It is used as listener when an UDP packet has been sent.
 */
public interface SenderListener {
    void onSent(UDPSenderService service, MessageModel messageModel);
    void onError(MessageModel senderService);
}
