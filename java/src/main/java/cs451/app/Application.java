package cs451.app;

import cs451.Constants;
import cs451.Host;
import cs451.Parser;
import cs451.interfaces.SendInterface;
import cs451.interfaces.Writer;
import cs451.perfectlink.Pp2pDeliver;
import cs451.udp.UDPReceiverService;
import cs451.uniformreliable.UrbDeliver;
import cs451.util.AbstractConfig;
import cs451.util.AbstractPrimitive;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/*
    This class has the objective of managing the entire application (both Receiver and Sender threads)
    It implements the functions to shutdown all threads and to finalize the log
 */

public class Application<C extends AbstractConfig, S extends AbstractPrimitive & SendInterface> implements Writer {
    //Configuration for a specific protocol (read from file)
    private final C config;

    //It must be an implementation of a sender primitive
    private final S send;

    private final List<Host> hosts;
    private final Host myHost;

    // Log file
    private final String outputLogFile;
    private final Set<String> log;

    // Core pool threads to receive and send packets
    private final ThreadPoolExecutor coreThreadSender;
    private final ThreadPoolExecutor coreThreadDeliver;

    // Thread pool to be used from the entire Application
    private final ThreadPoolExecutor deliveryThreads;
    private final ThreadPoolExecutor ackThreads;

    private final ThreadPoolExecutor senderThread;
    private final ThreadPoolExecutor addBroadcastMessagesThread;


    public Application(Class<C> configType, Class<S> sendType, Parser parser) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, SocketException {
        Constructor<C> configConstructor = configType.getConstructor(String.class);
        Constructor<S> sendConstructor = sendType.getConstructor(Host.class, List.class, Set.class, Writer.class, ThreadPoolExecutor.class);

        this.log = Collections.synchronizedSet(new LinkedHashSet<>());
        this.outputLogFile = parser.output();

        try {
            Files.deleteIfExists(Path.of(this.outputLogFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.hosts = parser.hosts();
        this.myHost = findHostById(parser.myId());

        assert myHost != null;

        this.senderThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
       // this.reBroadcastThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        this.addBroadcastMessagesThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

        this.coreThreadSender = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        this.coreThreadDeliver = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

        this.ackThreads = (ThreadPoolExecutor) Executors.newFixedThreadPool(Constants.ACK_THREAD_POOL_SIZE);
        this.deliveryThreads = (ThreadPoolExecutor) Executors.newFixedThreadPool(Constants.DELIVERY_THREAD_POOL_SIZE);

        this.config = configConstructor.newInstance(parser.config());

        UrbDeliver.setReBroadcastThreads(/*this.reBroadcastThread, */this.addBroadcastMessagesThread);

        Pp2pDeliver deliver = new Pp2pDeliver(this.myHost, this.hosts, this.log, this, this.deliveryThreads);
        this.send = sendConstructor.newInstance(this.myHost, this.hosts, this.log, this, this.ackThreads);

        if (!coreThreadDeliver.isShutdown()) {
            // Listening on specified port
            coreThreadDeliver.execute(new UDPReceiverService(myHost.getPort(), deliver::deliver));
        }

        if(!coreThreadSender.isShutdown()) {
            // Sending packets
            coreThreadSender.execute(send::send);
        }
    }

    public void sendMessage(Runnable action) {
        this.senderThread.execute(action);
    }

    public C getConfig() {
        return config;
    }

    public S getSend() {
        return send;
    }

    public Host getMyHost() {
        return this.myHost;
    }

    // Find an host in the list of all hosts by ID
    public Host findHostById(int id) {
        for (Host host : this.hosts) {
            if (host.getId() == id) {
                return host;
            }
        }

        return null;
    }

    // Stop all threads
    public void shutdown() {
        if (deliveryThreads != null)
            deliveryThreads.shutdownNow();

        if (ackThreads != null)
            ackThreads.shutdownNow();

        if (coreThreadSender != null)
            coreThreadSender.shutdownNow();

        if (coreThreadDeliver != null)
            coreThreadDeliver.shutdownNow();

        if(senderThread != null)
            senderThread.shutdown();

        /*if(reBroadcastThread != null)
            reBroadcastThread.shutdown();*/

        if(addBroadcastMessagesThread != null)
            addBroadcastMessagesThread.shutdown();
    }

    // Write the log queue in a file
    public void finalizeLog() {
        if (this.outputLogFile != null && this.log != null) {
            synchronized (this.log) {
                try {
                    Files.write(Paths.get(this.outputLogFile), this.log, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    this.log.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
