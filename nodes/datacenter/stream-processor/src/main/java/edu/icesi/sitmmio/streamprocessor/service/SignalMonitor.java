package edu.icesi.sitmmio.streamprocessor.service;

import edu.icesi.sitmmio.domain.Datagram;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SignalMonitor {
    private final Map<Integer, LocalDateTime> lastSeen = new ConcurrentHashMap<>();
    private final long thresholdSeconds;

    public SignalMonitor(long thresholdSeconds) { this.thresholdSeconds = thresholdSeconds; }

    public void record(Datagram d) { lastSeen.put(d.busId(), d.datagramDate()); }

    public boolean isSignalLost(int busId, LocalDateTime now) {
        LocalDateTime last = lastSeen.get(busId);
        if (last == null) return false;
        return Duration.between(last, now).getSeconds() > thresholdSeconds;
    }
}
