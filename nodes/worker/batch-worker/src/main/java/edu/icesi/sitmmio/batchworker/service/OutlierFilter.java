package edu.icesi.sitmmio.batchworker.service;

import java.time.Duration;

public final class OutlierFilter {
    private final double maxSpeedKmH;
    private final long maxGapSeconds;

    public OutlierFilter(double maxSpeedKmH, long maxGapSeconds) {
        this.maxSpeedKmH = maxSpeedKmH;
        this.maxGapSeconds = maxGapSeconds;
    }

    public static OutlierFilter defaults() {
        return new OutlierFilter(120.0, 5 * 60);
    }

    public boolean accept(double speedKmH, double distKm, Duration dt) {
        if (speedKmH < 0 || speedKmH > maxSpeedKmH) return false;
        if (dt.getSeconds() <= 0 || dt.getSeconds() > maxGapSeconds) return false;
        return true;
    }
}
