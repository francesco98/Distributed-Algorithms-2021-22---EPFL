package cs451;

import cs451.app.Application;
import cs451.app.ProtocolType;
import cs451.besteffort.BebSend;
import cs451.fifo.FIFOSend;
import cs451.model.AddressPortPair;
import cs451.model.PacketModel;
import cs451.perfectlink.Pp2pSend;
import cs451.uniformreliable.UrbSend;
import cs451.util.BestEffortConfig;
import cs451.util.FIFOConfig;
import cs451.util.PerfectLinkConfig;
import cs451.util.UniformReliableConfig;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    // It stores the running application
    private static Application<?, ?> app;

    private static void handleSignal() {
        // Immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        if (app != null)
            app.shutdown();

        // Write/flush output file if necessary
        System.out.println("Writing output.");

        if (app != null)
            app.finalizeLog();
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println(Runtime.getRuntime().availableProcessors());

        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");

        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        for (Host host : parser.hosts()) {
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
        }
        System.out.println();

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");

        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.config() + "\n");

        try {
            System.out.println("Doing some initialization\n");

            if (Constants.PROTOCOL_TYPE == ProtocolType.PERFECT_LINKS) {
                // Init the app
                app = new Application<>(PerfectLinkConfig.class, Pp2pSend.class, parser);

                // Getting the receiver
                Host receiver = app.findHostById(((PerfectLinkConfig) app.getConfig()).getReceiverId());

                System.out.println("Broadcasting and delivering messages...\n");

                // Sending messages if the host is not the receiver
                if (app.getMyHost().getId() != receiver.getId()) {
                    for (int i = 1; i <= app.getConfig().getNumberOfMessages(); i++) {
                        try {
                            PacketModel packetModel = new PacketModel(app.getMyHost().getId(), i, i + "");
                            packetModel.setDestinationId(receiver.getId());
                            AddressPortPair addressPortPair = new AddressPortPair(InetAddress.getByName(receiver.getIp()), receiver.getPort());
                            app.sendMessage(() -> {
                                ((Pp2pSend) app.getSend()).blockingAdd(addressPortPair, packetModel);
                            });
                        } catch (UnknownHostException e) {
                            System.out.println("Problem for host: " + app.getMyHost().getId() + " for packet: " + i);
                        }
                    }
                }
            } else if (Constants.PROTOCOL_TYPE == ProtocolType.BEST_EFFORT_BROADCAST) {
                // Init the app
                app = new Application<>(BestEffortConfig.class, BebSend.class, parser);

                System.out.println("Broadcasting and delivering messages...\n");

                for (int i = 1; i <= app.getConfig().getNumberOfMessages(); i++) {
                    PacketModel packetModel = new PacketModel(app.getMyHost().getId(), i, i + "");
                    app.sendMessage(() -> {
                        ((BebSend) app.getSend()).blockingAdd(packetModel);
                    });
                }
            } else if (Constants.PROTOCOL_TYPE == ProtocolType.UNIFORM_RELIABLE_BROADCAST) {
                // Init the app
                app = new Application<>(UniformReliableConfig.class, UrbSend.class, parser);

                System.out.println("Broadcasting and delivering messages...\n");

                for (int i = 1; i <= app.getConfig().getNumberOfMessages(); i++) {
                    PacketModel packetModel = new PacketModel(app.getMyHost().getId(), i, i + "");
                    app.sendMessage(() -> {
                        ((UrbSend) app.getSend()).blockingAdd(packetModel);
                    });
                }
            } else if(Constants.PROTOCOL_TYPE == ProtocolType.FIFO_BROADCAST) {
                // Init the app
                app = new Application<>(FIFOConfig.class, FIFOSend.class, parser);

                System.out.println("Broadcasting and delivering messages...\n");

                for (int i = 1; i <= app.getConfig().getNumberOfMessages(); i++) {
                    PacketModel packetModel = new PacketModel(app.getMyHost().getId(), i, i + "");
                    app.sendMessage(() -> {
                        ((FIFOSend) app.getSend()).blockingAdd(packetModel);
                    });
                }
            }

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | SocketException e) {
            e.printStackTrace();
            System.out.println("Process: " + parser.myId() + " cannot be instantiated \n");
        }

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
