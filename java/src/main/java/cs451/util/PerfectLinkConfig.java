package cs451.util;

/*
    It parses the perfect link configuration file. The field to be added to the number of messages is the receiver host.
 */
public class PerfectLinkConfig extends AbstractConfig {

    public PerfectLinkConfig(String filePath) {
        super(filePath);
    }

    public int getReceiverId() {
        return Integer.parseInt(lines.get(0)[1]);
    }
}
