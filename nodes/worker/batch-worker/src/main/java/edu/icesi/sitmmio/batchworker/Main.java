package edu.icesi.sitmmio.batchworker;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import edu.icesi.sitmmio.batchworker.adapter.BatchWorkerI;
import edu.icesi.sitmmio.domain.Route;
import edu.icesi.sitmmio.io.RouteCsvReader;

import java.nio.file.Path;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String workerId = "worker-" + System.currentTimeMillis();
        String lake = "lake";
        String routesPath = "data/raw/lines-241-ActiveGT.csv";
        int port = 10100 + (int)(System.currentTimeMillis() % 100);
        String masterProxy = "BatchMaster:default -h 127.0.0.1 -p 10050";
        for (int i = 0; i < args.length - 1; i++) {
            if ("--worker-id".equals(args[i])) workerId = args[i + 1];
            if ("--lake".equals(args[i])) lake = args[i + 1];
            if ("--routes".equals(args[i])) routesPath = args[i + 1];
            if ("--port".equals(args[i])) port = Integer.parseInt(args[i + 1]);
            if ("--master-proxy".equals(args[i])) masterProxy = args[i + 1];
        }
        try (Communicator c = Util.initialize(args)) {
            Map<Integer, Route> routes = new RouteCsvReader().readActiveRoutes(Path.of(routesPath));
            BatchWorkerI servant = new BatchWorkerI(workerId, Path.of(lake), routes);
            ObjectAdapter adapter = c.createObjectAdapterWithEndpoints(
                    "BatchWorkerAdapter-" + workerId, "default -p " + port);
            ObjectPrx selfPrx = adapter.add(servant, Util.stringToIdentity(workerId));
            adapter.activate();
            System.out.println("[batch-worker] id=" + workerId + " listening on " + port);

            // Auto-registro contra el Master
            try {
                SITM.BatchMasterPrx master = SITM.BatchMasterPrx.checkedCast(c.stringToProxy(masterProxy));
                if (master != null) {
                    master.registerWorker(SITM.IBatchWorkerPrx.uncheckedCast(selfPrx));
                    System.out.println("[batch-worker] registered with master");
                }
            } catch (Exception e) {
                System.out.println("[batch-worker] master not reachable; standalone mode");
            }
            c.waitForShutdown();
        } catch (Exception e) {
            System.err.println("[batch-worker] FATAL: " + e);
            System.exit(1);
        }
    }
}
