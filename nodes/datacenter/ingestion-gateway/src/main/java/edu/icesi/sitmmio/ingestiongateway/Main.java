package edu.icesi.sitmmio.ingestiongateway;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import edu.icesi.sitmmio.domain.Route;
import edu.icesi.sitmmio.ingestiongateway.adapter.IngestionGatewayI;
import edu.icesi.sitmmio.ingestiongateway.io.RoutesLoader;
import edu.icesi.sitmmio.ingestiongateway.service.RejectAuditor;
import edu.icesi.sitmmio.validation.DatagramValidator;

import java.nio.file.Path;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        String routesPath = "data/raw/lines-241-ActiveGT.csv";
        String queueProxy = "DatagramQueue:default -h 127.0.0.1 -p 10010";
        String archiveProxy = "ArchiveService:default -h 127.0.0.1 -p 10001";
        boolean queueEnabled = true;

        for (int i = 0; i < args.length; i++) {
            if ("--no-queue".equals(args[i])) {
                queueEnabled = false;
            } else if (i < args.length - 1) {
                if ("--routes".equals(args[i])) routesPath = args[i + 1];
                if ("--queue-proxy".equals(args[i])) queueProxy = args[i + 1];
                if ("--archive-proxy".equals(args[i])) archiveProxy = args[i + 1];
            }
        }

        try (Communicator c = Util.initialize(args)) {
            Map<Integer, Route> routes = RoutesLoader.load(Path.of(routesPath));
            System.out.println("[ingestion-gateway] loaded " + routes.size() + " active routes");

            SITM.DatagramQueuePrx queue = queueEnabled ? tryConnectQueue(c, queueProxy) : null;
            SITM.ArchiveServicePrx archive = tryConnectArchive(c, archiveProxy);
            if (!queueEnabled) {
                System.out.println("[ingestion-gateway] queue disabled for bulk ingestion");
            }

            DatagramValidator validator = new DatagramValidator();
            RejectAuditor auditor = new RejectAuditor();
            IngestionGatewayI servant = new IngestionGatewayI(
                    validator, routes, auditor, queue, archive);

            ObjectAdapter adapter = c.createObjectAdapterWithEndpoints(
                    "IngestionGatewayAdapter", "default -p 10000");
            adapter.add(servant, Util.stringToIdentity("DatagramReceiver"));
            adapter.activate();

            System.out.println("[ingestion-gateway] listening on port 10000");
            c.waitForShutdown();
        } catch (Exception e) {
            System.err.println("[ingestion-gateway] FATAL: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static SITM.DatagramQueuePrx tryConnectQueue(Communicator c, String proxy) {
        try {
            ObjectPrx p = c.stringToProxy(proxy);
            return SITM.DatagramQueuePrx.checkedCast(p);
        } catch (Exception e) {
            System.out.println("[ingestion-gateway] queue not available, will skip enqueue");
            return null;
        }
    }

    private static SITM.ArchiveServicePrx tryConnectArchive(Communicator c, String proxy) {
        try {
            ObjectPrx p = c.stringToProxy(proxy);
            return SITM.ArchiveServicePrx.checkedCast(p);
        } catch (Exception e) {
            System.out.println("[ingestion-gateway] archive not available, will skip archive");
            return null;
        }
    }
}
