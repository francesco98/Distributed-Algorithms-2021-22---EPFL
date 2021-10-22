package cs451;

import cs451.app.Application;
import cs451.app.ProtocolType;
import cs451.model.MessageModel;
import cs451.model.PacketModel;
import cs451.perfectlink.Pp2pDeliver;
import cs451.perfectlink.Pp2pSend;
import cs451.util.PerfectLinkConfig;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    // It stores the running application
    private static Application<PerfectLinkConfig, Pp2pSend, Pp2pDeliver> app;

    private static void handleSignal() {
        // Immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        if(app != null)
            app.shutdown();

        // Write/flush output file if necessary
        System.out.println("Writing output.");

        if(app != null)
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
            if (Constants.PROTOCOL_TYPE == ProtocolType.PERFECT_LINKS) {

                System.out.println("Doing some initialization\n");

                // Init the app
                app = new Application<>(PerfectLinkConfig.class, Pp2pSend.class, Pp2pDeliver.class, parser);

                // Getting the receiver
                Host receiver = app.findHostById(app.getConfig().getReceiverId());

                System.out.println("Broadcasting and delivering messages...\n");

                // Sending messages if the host is not the receiver
                if (app.getMyHost().getId() != receiver.getId()) {
                    for (int i = 1; i <= app.getConfig().getNumberOfMessages(); i++) {
                        try {
                            PacketModel packetModel = new PacketModel(app.getMyHost().getId(), receiver.getId(), i, i + "");
                            MessageModel messageModel = new MessageModel(
                                    InetAddress.getByName(receiver.getIp()), receiver.getPort(), packetModel);

                            app.getSend().add(messageModel);
                        } catch (UnknownHostException e) {
                            System.out.println("Problem for host: " + app.getMyHost().getId() + " for packet: " + i);
                        }
                    }
                }
            }

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | SocketException e) {
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
