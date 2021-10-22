package cs451;

import cs451.app.ProtocolType;

public class Constants {
    public static final int ARG_LIMIT_CONFIG = 7;

    // indexes for id
    public static final int ID_KEY = 0;
    public static final int ID_VALUE = 1;

    // indexes for hosts
    public static final int HOSTS_KEY = 2;
    public static final int HOSTS_VALUE = 3;

    // indexes for output
    public static final int OUTPUT_KEY = 4;
    public static final int OUTPUT_VALUE = 5;

    // indexes for config
    public static final int CONFIG_VALUE = 6;

    public final static int MAX_PACKET_LEN = 128;
    public static final ProtocolType PROTOCOL_TYPE = ProtocolType.PERFECT_LINKS;

    public static final int ACK_TIMEOUT_MS = 10 * 1000; // 10ms ack timeout

    public static final int OPEN_SOCKET_LIMIT = 50;

    public static final int CORE_THREAD_POOL_SIZE = 2;
    public static final int ACK_THREAD_POOL_SIZE = 60;
    public static final int DELIVERY_THREAD_POOL_SIZE = 60;

}
