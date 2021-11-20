package cs451.uniformreliable;

import cs451.Host;
import cs451.besteffort.BebSend;
import cs451.interfaces.SendInterface;
import cs451.interfaces.Writer;
import cs451.model.PacketModel;
import cs451.util.AbstractPrimitive;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class UrbSend extends AbstractPrimitive implements SendInterface {
    private final BebSend bebSend;

    public UrbSend(Host host, List<Host> hosts, Set<String> log, Writer writer, ThreadPoolExecutor executorService) {
        super(host, hosts, log, writer, executorService);

        this.bebSend = new BebSend(host, hosts, log, writer, executorService);
        UrbDeliver.setBebSend(this.bebSend);
    }

    public void blockingAdd(PacketModel packetModel) {
        bebSend.blockingAdd(packetModel);
    }

    @Override
    public void send() {
        bebSend.send();
    }
}
