package edu.icesi.sitmmio.bussimulator.service;

public final class EmissionScheduler {
    private final long throttleMs;
    private final double rateMultiplier;

    public EmissionScheduler(long throttleMs, double rateMultiplier) {
        this.throttleMs = throttleMs;
        this.rateMultiplier = rateMultiplier;
    }

    /** Espera el intervalo configurado entre emisiones. */
    public void pace() throws InterruptedException {
        if (throttleMs <= 0) return;
        long effective = (long) (throttleMs / Math.max(rateMultiplier, 0.001));
        if (effective > 0) Thread.sleep(effective);
    }
}
