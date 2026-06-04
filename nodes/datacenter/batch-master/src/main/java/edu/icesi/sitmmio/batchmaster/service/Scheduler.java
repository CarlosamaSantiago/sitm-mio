package edu.icesi.sitmmio.batchmaster.service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

public final class Scheduler {
    private final WorkerRegistry registry;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final LongAdder completed = new LongAdder();
    private final LongAdder reassigned = new LongAdder();
    private final long timeoutSeconds;
    private final int maxAttempts;

    public Scheduler(WorkerRegistry registry, long timeoutSeconds, int maxAttempts) {
        this.registry = registry;
        this.timeoutSeconds = timeoutSeconds;
        this.maxAttempts = maxAttempts;
    }

    public CompletableFuture<SITM.SpeedReport> assign(SITM.PartitionKey k) {
        return CompletableFuture.supplyAsync(() -> runWithRetry(k), pool);
    }

    private SITM.SpeedReport runWithRetry(SITM.PartitionKey k) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            SITM.IBatchWorkerPrx w = registry.pickRoundRobin();
            if (w == null) throw new IllegalStateException("no workers");
            try {
                CompletableFuture<SITM.SpeedReport> f = CompletableFuture.supplyAsync(
                        () -> {
                            try { return w.computePartition(k); }
                            catch (SITM.NoDataForPartition e) {
                                SITM.SpeedReport r = new SITM.SpeedReport();
                                r.lineId = k.lineId; r.year = k.year; r.month = k.month;
                                r.shortName = "UNKNOWN"; r.description = "NA"; r.status = "NO_DATA";
                                return r;
                            }
                        }, pool);
                SITM.SpeedReport result = f.get(timeoutSeconds, TimeUnit.SECONDS);
                completed.increment();
                return result;
            } catch (Exception e) {
                reassigned.increment();
            }
        }
        // Tras maxAttempts, devolvemos NO_DATA en lugar de explotar
        SITM.SpeedReport r = new SITM.SpeedReport();
        r.lineId = k.lineId; r.year = k.year; r.month = k.month;
        r.shortName = "UNKNOWN"; r.description = "NA"; r.status = "NO_DATA";
        return r;
    }

    public long completed()  { return completed.sum(); }
    public long reassigned() { return reassigned.sum(); }
    public void shutdown() { pool.shutdownNow(); }
}
