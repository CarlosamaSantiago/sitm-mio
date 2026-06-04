package edu.icesi.sitmmio.batchworker.service;

import java.time.Duration;

public final class OutlierFilter {
    private final double maxSpeedKmH;
    private final long maxGapSeconds;
    private final double maxJumpKmIn30s;

    public OutlierFilter(double maxSpeedKmH, long maxGapSeconds, double maxJumpKmIn30s) {
        this.maxSpeedKmH = maxSpeedKmH;
        this.maxGapSeconds = maxGapSeconds;
        this.maxJumpKmIn30s = maxJumpKmIn30s;
    }
    public static OutlierFilter defaults() { return new OutlierFilter(80.0, 60 * 60, 5.0); }

    public boolean accept(double speedKmH, double distKm, Duration dt) {
        if (speedKmH < 0 || speedKmH > maxSpeedKmH) return false;
        if (dt.getSeconds() <= 0 || dt.getSeconds() > maxGapSeconds) return false;
        if (dt.getSeconds() < 30 && distKm > maxJumpKmIn30s) return false;
        return true;
    }
}
