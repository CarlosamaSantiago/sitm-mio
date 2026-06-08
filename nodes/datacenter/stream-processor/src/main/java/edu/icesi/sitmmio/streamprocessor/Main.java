package edu.icesi.sitmmio.streamprocessor;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import edu.icesi.sitmmio.streamprocessor.adapter.EventBusPublisher;
import edu.icesi.sitmmio.streamprocessor.adapter.QueueConsumer;
import edu.icesi.sitmmio.streamprocessor.service.BusStateBuilder;
import edu.icesi.sitmmio.streamprocessor.service.DatagramExtractor;
import edu.icesi.sitmmio.streamprocessor.service.EventClassifier;
import edu.icesi.sitmmio.streamprocessor.service.SignalMonitor;

public class Main {
    public static void main(String[] args) {
        String queueProxy = "DatagramQueue:default -h 127.0.0.1 -p 10010";
        String busProxy = "DatagramEventBus:default -h 127.0.0.1 -p 10020";
        String sessionProxy = "SessionContextController:default -h 127.0.0.1 -p 10030";
        int consumers = 1;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--queue-proxy".equals(args[i])) queueProxy = args[i + 1];
            if ("--bus-proxy".equals(args[i])) busProxy = args[i + 1];
            if ("--session-proxy".equals(args[i])) sessionProxy = args[i + 1];
            if ("--consumers".equals(args[i])) consumers = Integer.parseInt(args[i + 1]);
        }
        try (Communicator c = Util.initialize(args)) {
            SITM.DatagramQueuePrx queue = SITM.DatagramQueuePrx.checkedCast(c.stringToProxy(queueProxy));
            if (queue == null) throw new IllegalStateException("invalid DatagramQueue proxy");

            SITM.DatagramEventBusPrx bus = tryCast(c, busProxy, SITM.DatagramEventBusPrx.class);
            SITM.SessionContextControllerPrx session = tryCast(c, sessionProxy, SITM.SessionContextControllerPrx.class);

            EventClassifier cls = new EventClassifier();
            DatagramExtractor ext = new DatagramExtractor();
            SignalMonitor mon = new SignalMonitor(60);
            BusStateBuilder st = new BusStateBuilder(cls);
            EventBusPublisher pub = new EventBusPublisher(bus);

            for (int i = 0; i < consumers; i++) {
                QueueConsumer worker = new QueueConsumer(queue, pub, ext, cls, mon, st, session);
                Thread t = new Thread(worker, "stream-consumer-" + i);
                t.setDaemon(false);
                t.start();
            }
            System.out.println("[stream-processor] running consumers=" + consumers);
            c.waitForShutdown();
        } catch (Exception e) {
            System.err.println("[stream-processor] FATAL: " + e);
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T tryCast(Communicator c, String proxy, Class<T> ignore) {
        try {
            ObjectPrx p = c.stringToProxy(proxy);
            if (ignore == SITM.DatagramEventBusPrx.class) return (T) SITM.DatagramEventBusPrx.checkedCast(p);
            if (ignore == SITM.SessionContextControllerPrx.class) return (T) SITM.SessionContextControllerPrx.checkedCast(p);
        } catch (Exception ignored) {}
        return null;
    }
}
