package edu.icesi.sitmmio.datagrameventbus;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import edu.icesi.sitmmio.datagrameventbus.adapter.DatagramEventBusI;
import edu.icesi.sitmmio.datagrameventbus.domain.SubscriberRegistry;
import edu.icesi.sitmmio.datagrameventbus.service.Dispatcher;

public class Main {
    public static void main(String[] args) {
        int port = 10020;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equals(args[i])) port = Integer.parseInt(args[i + 1]);
        }
        try (Communicator c = Util.initialize(args)) {
            SubscriberRegistry reg = new SubscriberRegistry();
            Dispatcher d = new Dispatcher(reg);
            DatagramEventBusI servant = new DatagramEventBusI(reg, d);
            ObjectAdapter adapter = c.createObjectAdapterWithEndpoints(
                    "DatagramEventBusAdapter", "default -p " + port);
            adapter.add(servant, Util.stringToIdentity("DatagramEventBus"));
            adapter.activate();
            System.out.println("[datagram-event-bus] listening on " + port);
            Runtime.getRuntime().addShutdownHook(new Thread(d::shutdown));
            c.waitForShutdown();
        } catch (Exception e) {
            System.err.println("[datagram-event-bus] FATAL: " + e);
            System.exit(1);
        }
    }
}
