package cs451.model;

import java.net.InetAddress;
import java.util.Objects;

public class AddressPortPair {
    private final InetAddress IPAddress;
    private final int port;

    public AddressPortPair(InetAddress IPAddress, int port) {
        this.IPAddress = IPAddress;
        this.port = port;
    }

    public InetAddress getIPAddress() {
        return IPAddress;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressPortPair that = (AddressPortPair) o;
        return port == that.port && IPAddress.equals(that.IPAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(IPAddress, port);
    }
}
