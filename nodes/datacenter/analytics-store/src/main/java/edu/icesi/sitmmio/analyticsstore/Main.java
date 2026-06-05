package edu.icesi.sitmmio.analyticsstore;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import edu.icesi.sitmmio.analyticsstore.adapter.AnalyticsStoreI;
import edu.icesi.sitmmio.analyticsstore.domain.ReportIndex;
import edu.icesi.sitmmio.analyticsstore.io.CsvBackend;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        int port = 10060;
        String loadCsv = null;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equals(args[i])) port = Integer.parseInt(args[i + 1]);
            if ("--load-csv".equals(args[i])) loadCsv = args[i + 1];
        }
        try (Communicator c = Util.initialize(args)) {
            ReportIndex index = new ReportIndex();
            if (loadCsv != null && !loadCsv.isBlank()) {
                int loaded = new CsvBackend(Path.of(loadCsv)).loadInto(index);
                System.out.println("[analytics-store] loaded reports=" + loaded + " from " + loadCsv);
            }
            AnalyticsStoreI servant = new AnalyticsStoreI(index);
            ObjectAdapter adapter = c.createObjectAdapterWithEndpoints(
                    "AnalyticsStoreAdapter", "default -p " + port);
            adapter.add(servant, Util.stringToIdentity("ReportProvider"));
            adapter.activate();
            System.out.println("[analytics-store] listening on " + port);
            c.waitForShutdown();
        } catch (Exception e) {
            System.err.println("[analytics-store] FATAL: " + e);
            System.exit(1);
        }
    }
}
