package edu.icesi.sitmmio.datagrameventbus.service;

import edu.icesi.sitmmio.datagrameventbus.domain.SubscriberRegistry;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;

public final class Dispatcher {

    private final SubscriberRegistry registry;
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final LongAdder updatesPublished = new LongAdder();
    private final LongAdder alertsPublished = new LongAdder();
    private final LongAdder deliveryFailures = new LongAdder();

    public Dispatcher(SubscriberRegistry registry) { this.registry = registry; }

    public void dispatchUpdate(SITM.BusUpdate u) {
        Map<SITM.MonitoringSubscriberPrx, Set<Integer>> snapshot = registry.snapshot();
        for (var e : snapshot.entrySet()) {
            Set<Integer> zones = e.getValue();
            if (zones.contains(0) || zones.contains(u.zoneId)) {
                pool.execute(() -> safeDeliverUpdate(e.getKey(), u));
            }
        }
        updatesPublished.increment();
    }

    public void dispatchAlert(SITM.CriticAlert a) {
        boolean broadcast = "ALTA".equals(a.priority);
        Map<SITM.MonitoringSubscriberPrx, Set<Integer>> snapshot = registry.snapshot();
        for (var e : snapshot.entrySet()) {
            Set<Integer> zones = e.getValue();
            if (broadcast || zones.contains(0) || zones.contains(a.zoneId)) {
                pool.execute(() -> safeDeliverAlert(e.getKey(), a));
            }
        }
        alertsPublished.increment();
    }

    private void safeDeliverUpdate(SITM.MonitoringSubscriberPrx sub, SITM.BusUpdate u) {
        try { sub.updateLocation(u); }
        catch (Exception e) { deliveryFailures.increment(); registry.unsubscribe(sub); }
    }

    private void safeDeliverAlert(SITM.MonitoringSubscriberPrx sub, SITM.CriticAlert a) {
        try { sub.onCriticAlert(a); }
        catch (Exception e) { deliveryFailures.increment(); registry.unsubscribe(sub); }
    }

    public long updates()  { return updatesPublished.sum(); }
    public long alerts()   { return alertsPublished.sum(); }
    public long failures() { return deliveryFailures.sum(); }

    public void shutdown() { pool.shutdownNow(); }
}
