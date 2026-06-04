package edu.icesi.sitmmio.datalake;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import edu.icesi.sitmmio.datalake.adapter.ArchiveServiceI;
import edu.icesi.sitmmio.datalake.io.FileLakeStore;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        String storeDir = "lake";
        int port = 10001;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--store".equals(args[i])) storeDir = args[i + 1];
            if ("--port".equals(args[i])) port = Integer.parseInt(args[i + 1]);
        }
        try (Communicator c = Util.initialize(args)) {
            FileLakeStore store = new FileLakeStore(Path.of(storeDir));
            ArchiveServiceI servant = new ArchiveServiceI(store);
            ObjectAdapter adapter = c.createObjectAdapterWithEndpoints(
                    "ArchiveServiceAdapter", "default -p " + port);
            adapter.add(servant, Util.stringToIdentity("ArchiveService"));
            adapter.activate();
            System.out.println("[data-lake] listening on " + port + " store=" + storeDir);
            Runtime.getRuntime().addShutdownHook(new Thread(store::close));
            c.waitForShutdown();
        } catch (Exception e) {
            System.err.println("[data-lake] FATAL: " + e);
            System.exit(1);
        }
    }
}
