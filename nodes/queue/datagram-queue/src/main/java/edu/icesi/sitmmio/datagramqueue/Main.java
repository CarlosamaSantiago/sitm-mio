package edu.icesi.sitmmio.datagramqueue;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import edu.icesi.sitmmio.datagramqueue.adapter.DatagramQueueI;
import edu.icesi.sitmmio.datagramqueue.domain.QueuePolicy;
import edu.icesi.sitmmio.datagramqueue.io.AppendOnlyStore;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        String storeDir = "queue/store";
        int port = 10010;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--store".equals(args[i])) storeDir = args[i + 1];
            if ("--port".equals(args[i])) port = Integer.parseInt(args[i + 1]);
        }
        try (Communicator c = Util.initialize(args)) {
            AppendOnlyStore store = new AppendOnlyStore(Path.of(storeDir), QueuePolicy.defaults());
            DatagramQueueI servant = new DatagramQueueI(store);
            ObjectAdapter adapter = c.createObjectAdapterWithEndpoints(
                    "DatagramQueueAdapter", "default -p " + port);
            adapter.add(servant, Util.stringToIdentity("DatagramQueue"));
            adapter.activate();
            System.out.println("[datagram-queue] listening on " + port + " store=" + storeDir);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { store.close(); } catch (Exception ignored) {}
            }));
            c.waitForShutdown();
        } catch (Exception e) {
            System.err.println("[datagram-queue] FATAL: " + e);
            System.exit(1);
        }
    }
}
