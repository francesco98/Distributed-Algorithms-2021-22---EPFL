package cs451.besteffort;

import cs451.Host;
import cs451.interfaces.SendInterface;
import cs451.model.AddressPortPair;
import cs451.model.PacketModel;
import cs451.perfectlink.Pp2pSend;
import cs451.util.AbstractPrimitive;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class BebSend extends AbstractPrimitive implements SendInterface {
    private final Pp2pSend pp2pSend;

    public BebSend(Host host, List<Host> hosts, Set<String> log, ThreadPoolExecutor executorService) {
        super(host, hosts, log, executorService);
        this.pp2pSend = new Pp2pSend(host, hosts, log, executorService);
    }

    public void blockingAdd(PacketModel packetModel) {
        this.add(packetModel, true);
    }

    public void add(PacketModel packetModel) {
        this.add(packetModel, false);
    }

    private void add(PacketModel packetModel, boolean blocking) {
        for (Host h : hosts) {
            try {
                PacketModel pm = new PacketModel(packetModel.toBytes(), host.getId());
                pm.setDestinationId(h.getId());
                if(blocking)
                    pp2pSend.blockingAdd(new AddressPortPair(InetAddress.getByName(h.getIp()), h.getPort()), pm);
                else
                    pp2pSend.add(new AddressPortPair(InetAddress.getByName(h.getIp()), h.getPort()), pm);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

        }
    }


    @Override
    public void send() {
        pp2pSend.send();
    }

}
