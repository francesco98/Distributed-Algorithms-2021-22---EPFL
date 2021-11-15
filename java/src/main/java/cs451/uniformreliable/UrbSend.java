package cs451.uniformreliable;

import cs451.Host;
import cs451.besteffort.BebSend;
import cs451.interfaces.SendInterface;
import cs451.model.PacketModel;
import cs451.util.AbstractPrimitive;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class UrbSend extends AbstractPrimitive implements SendInterface {
    private final BebSend bebSend;

    public UrbSend(Host host, List<Host> hosts, Set<String> log, ThreadPoolExecutor executorService) {
        super(host, hosts, log, executorService);

        this.bebSend = new BebSend(host, hosts, log, executorService);
        //UrbDeliver.setBebSend(this.bebSend);
    }

    public void blockingAdd(PacketModel packetModel) {
        bebSend.blockingAdd(packetModel);
    }

    /*public void add(PacketModel packetModel) {
        bebSend.add(packetModel);
    }*/

    @Override
    public void send() {
        bebSend.send();
    }
}
