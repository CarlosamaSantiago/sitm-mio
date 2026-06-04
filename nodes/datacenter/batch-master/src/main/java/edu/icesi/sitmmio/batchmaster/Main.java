package edu.icesi.sitmmio.batchmaster;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import edu.icesi.sitmmio.batchmaster.adapter.BatchMasterI;
import edu.icesi.sitmmio.batchmaster.service.Scheduler;
import edu.icesi.sitmmio.batchmaster.service.WorkerRegistry;
import edu.icesi.sitmmio.domain.Route;
import edu.icesi.sitmmio.io.RouteCsvReader;

import java.nio.file.Path;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        int port = 10050;
        String routesPath = "data/raw/lines-241-ActiveGT.csv";
        long timeoutSec = 120;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equals(args[i])) port = Integer.parseInt(args[i + 1]);
            if ("--routes".equals(args[i])) routesPath = args[i + 1];
            if ("--timeout-s".equals(args[i])) timeoutSec = Long.parseLong(args[i + 1]);
        }
        try (Communicator c = Util.initialize(args)) {
            Map<Integer, Route> routes = new RouteCsvReader().readActiveRoutes(Path.of(routesPath));
            System.out.println("[batch-master] active routes=" + routes.size());

            WorkerRegistry registry = new WorkerRegistry();
            Scheduler scheduler = new Scheduler(registry, timeoutSec, 3);
            BatchMasterI servant = new BatchMasterI(registry, scheduler, routes.keySet());

            ObjectAdapter adapter = c.createObjectAdapterWithEndpoints(
                    "BatchMasterAdapter", "default -p " + port);
            adapter.add(servant, Util.stringToIdentity("BatchMaster"));
            adapter.activate();
            System.out.println("[batch-master] listening on " + port);
            Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdown));
            c.waitForShutdown();
        } catch (Exception e) {
            System.err.println("[batch-master] FATAL: " + e);
            System.exit(1);
        }
    }
}
