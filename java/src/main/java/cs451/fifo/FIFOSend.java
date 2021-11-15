package cs451.fifo;

import cs451.Host;
import cs451.interfaces.SendInterface;
import cs451.model.PacketModel;
import cs451.uniformreliable.UrbSend;
import cs451.util.AbstractPrimitive;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class FIFOSend extends AbstractPrimitive implements SendInterface {
    private final UrbSend urbSend;

    public FIFOSend(Host host, List<Host> hosts, Set<String> log, ThreadPoolExecutor executorService) {
        super(host, hosts, log, executorService);
        this.urbSend = new UrbSend(host, hosts, log, executorService);
    }

    public void blockingAdd(PacketModel packetModel) {
        this.urbSend.blockingAdd(packetModel);
    }

    @Override
    public void send() {
        this.urbSend.send();
    }
}
