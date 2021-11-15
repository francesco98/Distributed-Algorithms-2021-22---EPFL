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

    public final static int MAX_QUEUE_PACKETS = 500;
    public final static int MAX_BUCKET_LEN = 1500;
    public static final ProtocolType PROTOCOL_TYPE = ProtocolType.FIFO_BROADCAST;

    public static final int ACK_TIMEOUT_MS = 500;

    public static final int OPEN_SOCKET_LIMIT = 2;

    public static final int CORE_THREAD_POOL_SIZE = 2;
    public static final int ACK_THREAD_POOL_SIZE = 4;
    public static final int DELIVERY_THREAD_POOL_SIZE = 8;

}
