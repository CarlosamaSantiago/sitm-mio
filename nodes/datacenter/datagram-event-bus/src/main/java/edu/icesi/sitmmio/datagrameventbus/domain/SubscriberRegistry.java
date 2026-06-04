package edu.icesi.sitmmio.datagrameventbus.domain;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public final class SubscriberRegistry {
    private final Map<SITM.MonitoringSubscriberPrx, Set<Integer>> subs = new ConcurrentHashMap<>();

    public void subscribe(SITM.MonitoringSubscriberPrx sub, int zoneId) {
        subs.computeIfAbsent(sub, k -> new CopyOnWriteArraySet<>()).add(zoneId);
    }

    public void unsubscribe(SITM.MonitoringSubscriberPrx sub) { subs.remove(sub); }

    public Map<SITM.MonitoringSubscriberPrx, Set<Integer>> snapshot() { return Map.copyOf(subs); }

    public int size() { return subs.size(); }
}
