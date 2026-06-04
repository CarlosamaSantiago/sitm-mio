package edu.icesi.sitmmio.bussimulator;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.LocalException;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import edu.icesi.sitmmio.bussimulator.io.ZipCsvStream;
import edu.icesi.sitmmio.bussimulator.service.CliArgs;
import edu.icesi.sitmmio.bussimulator.service.EmissionScheduler;
import edu.icesi.sitmmio.contracts.SliceMapper;
import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.io.DatagramCsvReader;

import java.nio.file.Path;
import java.util.Optional;

public class Main {

    public static void main(String[] args) {
        CliArgs cli = CliArgs.parse(args);
        String host       = cli.str("host", "127.0.0.1");
        int port          = cli.intv("port", 10000);
        String dataset    = cli.str("dataset", "data/raw/datagrams-MiniPilot.csv");
        long throttleMs   = cli.longv("throttle-ms", 0);
        double rateMult   = cli.dbl("rate-multiplier", 1.0);
        long maxRecords   = cli.longv("max-records", Long.MAX_VALUE);

        System.out.println("[bus-simulator] host=" + host + " port=" + port
                + " dataset=" + dataset + " throttle=" + throttleMs + "ms rate=" + rateMult
                + " max=" + (maxRecords == Long.MAX_VALUE ? "infinito" : String.valueOf(maxRecords)));

        DatagramCsvReader parser = new DatagramCsvReader();
        EmissionScheduler scheduler = new EmissionScheduler(throttleMs, rateMult);

        try (Communicator communicator = Util.initialize(args)) {
            com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy(
                    "DatagramReceiver:default -h " + host + " -p " + port);
            SITM.DatagramReceiverPrx receiver = SITM.DatagramReceiverPrx.checkedCast(base);
            if (receiver == null) throw new IllegalStateException("invalid DatagramReceiver proxy");

            long sent = 0, parseSkipped = 0, rejected = 0, ioErrors = 0, total = 0;
            long startNs = System.nanoTime();
            long lastLogNs = startNs;

            try (ZipCsvStream stream = ZipCsvStream.open(Path.of(dataset))) {
                var it = stream.lines().iterator();
                while (it.hasNext() && total < maxRecords) {
                    String line = it.next();
                    total++;
                    Optional<Datagram> parsed = parser.parseLine(line);
                    if (parsed.isEmpty()) { parseSkipped++; logIfNeeded(sent, rejected, parseSkipped, total, startNs, lastLogNs); lastLogNs = System.nanoTime(); continue; }
                    try {
                        receiver.postDatagram(SliceMapper.toSlice(parsed.get()));
                        sent++;
                    } catch (SITM.InvalidDatagram e) {
                        rejected++;
                    } catch (LocalException e) {
                        ioErrors++;
                        if (ioErrors > 100) { System.err.println("[bus-simulator] demasiados errores Ice, abortando"); break; }
                    } catch (java.lang.Exception e) {
                        ioErrors++;
                    }
                    if (total % 10_000 == 0) {
                        long now = System.nanoTime();
                        double sec = (now - startNs) / 1e9;
                        System.out.printf("[bus-simulator] total=%d sent=%d rejected=%d parse_skipped=%d io_errors=%d throughput=%.0f/s%n",
                                total, sent, rejected, parseSkipped, ioErrors, total / sec);
                        lastLogNs = now;
                    }
                    try { scheduler.pace(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }

            double elapsedSec = (System.nanoTime() - startNs) / 1e9;
            System.out.println();
            System.out.println("==============================================");
            System.out.println(" [bus-simulator] DONE");
            System.out.printf ("   Total leidos:      %d%n", total);
            System.out.printf ("   Enviados OK:       %d%n", sent);
            System.out.printf ("   Rechazados R35:    %d%n", rejected);
            System.out.printf ("   Parse skipped:     %d%n", parseSkipped);
            System.out.printf ("   IO errors:         %d%n", ioErrors);
            System.out.printf ("   Tiempo:            %.2f s%n", elapsedSec);
            System.out.printf ("   Throughput:        %.0f rows/s%n", total > 0 ? total / elapsedSec : 0);
            System.out.println("==============================================");
        } catch (java.lang.Exception e) {
            System.err.println("[bus-simulator] FATAL: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void logIfNeeded(long sent, long rejected, long skipped, long total, long startNs, long lastLogNs) {
        long now = System.nanoTime();
        if ((now - lastLogNs) > 5_000_000_000L) { // cada 5s aunque no cambien múltiplos de 10k
            double sec = (now - startNs) / 1e9;
            System.out.printf("[bus-simulator] heartbeat total=%d sent=%d rejected=%d parse=%d throughput=%.0f/s%n",
                    total, sent, rejected, skipped, total / sec);
        }
    }
}
