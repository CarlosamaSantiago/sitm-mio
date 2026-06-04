package edu.icesi.sitmmio.sessioncontext;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import edu.icesi.sitmmio.sessioncontext.adapter.SessionContextControllerI;
import edu.icesi.sitmmio.sessioncontext.domain.ZoneMap;
import edu.icesi.sitmmio.sessioncontext.io.ZoneMappingLoader;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        int port = 10030;
        String mapping = "admin/zone-mapping.example.json";
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equals(args[i])) port = Integer.parseInt(args[i + 1]);
            if ("--mapping".equals(args[i])) mapping = args[i + 1];
        }
        try (Communicator c = Util.initialize(args)) {
            ZoneMap m = ZoneMappingLoader.load(Path.of(mapping));
            SessionContextControllerI servant = new SessionContextControllerI(m);
            ObjectAdapter adapter = c.createObjectAdapterWithEndpoints(
                    "SessionContextAdapter", "default -p " + port);
            adapter.add(servant, Util.stringToIdentity("SessionContextController"));
            adapter.activate();
            System.out.println("[session-context] listening on " + port);
            c.waitForShutdown();
        } catch (Exception e) {
            System.err.println("[session-context] FATAL: " + e);
            System.exit(1);
        }
    }
}
