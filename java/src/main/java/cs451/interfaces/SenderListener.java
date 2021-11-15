package cs451.interfaces;

import cs451.model.BucketModel;
import cs451.udp.UDPSenderService;

/*
    It is used as listener when an UDP packet has been sent.
 */
public interface SenderListener {
    void onSent(UDPSenderService service, BucketModel messageModel);
    void onError(BucketModel senderService);
}
