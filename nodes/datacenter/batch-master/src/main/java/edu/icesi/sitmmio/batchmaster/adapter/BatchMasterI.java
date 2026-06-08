package edu.icesi.sitmmio.batchmaster.adapter;

import com.zeroc.Ice.Current;
import edu.icesi.sitmmio.batchmaster.service.Partitioner;
import edu.icesi.sitmmio.batchmaster.service.Scheduler;
import edu.icesi.sitmmio.batchmaster.service.WorkerRegistry;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class BatchMasterI implements SITM.BatchMaster {

    private final WorkerRegistry registry;
    private final Scheduler scheduler;
    private final Partitioner partitioner = new Partitioner();
    private final Set<Integer> activeLineIds;

    public BatchMasterI(WorkerRegistry registry, Scheduler scheduler, Set<Integer> activeLineIds) {
        this.registry = registry;
        this.scheduler = scheduler;
        this.activeLineIds = activeLineIds;
    }

    @Override
    public void registerWorker(SITM.IBatchWorkerPrx w, Current current) {
        registry.register(w);
        System.out.println("[batch-master] worker registered. total=" + registry.size());
    }

    @Override
    public SITM.SpeedReport[] runMonth(int year, int month, Current current) {
        List<SITM.PartitionKey> keys = partitioner.forMonth(activeLineIds, year, month);
        List<CompletableFuture<SITM.SpeedReport>> futures = keys.stream()
                .map(this::assignToAllWorkers)
                .collect(Collectors.toList());
        SITM.SpeedReport[] out = new SITM.SpeedReport[futures.size()];
        for (int i = 0; i < futures.size(); i++) {
            try { out[i] = futures.get(i).get(); }
            catch (Exception e) {
                SITM.SpeedReport r = new SITM.SpeedReport();
                r.lineId = keys.get(i).lineId; r.year = year; r.month = month;
                r.shortName = "UNKNOWN"; r.description = "NA"; r.status = "NO_DATA";
                out[i] = r;
            }
        }
        return out;
    }

    private CompletableFuture<SITM.SpeedReport> assignToAllWorkers(SITM.PartitionKey key) {
        List<SITM.IBatchWorkerPrx> workers = registry.snapshot();
        if (workers.isEmpty()) {
            return CompletableFuture.completedFuture(noData(key));
        }

        List<CompletableFuture<SITM.SpeedReport>> partials = workers.stream()
                .map(worker -> CompletableFuture.supplyAsync(() -> computePartial(worker, key)))
                .toList();
        return CompletableFuture.allOf(partials.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> mergePartials(key, partials));
    }

    private SITM.SpeedReport computePartial(SITM.IBatchWorkerPrx worker, SITM.PartitionKey key) {
        try {
            return worker.computePartition(key);
        } catch (Exception ignored) {
            return noData(key);
        }
    }

    private SITM.SpeedReport mergePartials(
            SITM.PartitionKey key,
            List<CompletableFuture<SITM.SpeedReport>> partials
    ) {
        SITM.SpeedReport merged = noData(key);
        for (CompletableFuture<SITM.SpeedReport> future : partials) {
            SITM.SpeedReport partial;
            try {
                partial = future.get();
            } catch (Exception ignored) {
                partial = noData(key);
            }
            if (partial.shortName != null && !"UNKNOWN".equals(partial.shortName)) {
                merged.shortName = partial.shortName;
                merged.description = partial.description;
            }
            merged.totalDistanceKm += partial.totalDistanceKm;
            merged.totalTimeHours += partial.totalTimeHours;
            merged.validSegments += partial.validSegments;
            merged.skippedSegments += partial.skippedSegments;
        }
        if (merged.validSegments > 0 && merged.totalTimeHours > 0.0) {
            merged.averageSpeedKmH = merged.totalDistanceKm / merged.totalTimeHours;
            merged.status = "OK";
        }
        return merged;
    }

    private SITM.SpeedReport noData(SITM.PartitionKey key) {
        SITM.SpeedReport r = new SITM.SpeedReport();
        r.lineId = key.lineId;
        r.year = key.year;
        r.month = key.month;
        r.shortName = "UNKNOWN";
        r.description = "NA";
        r.status = "NO_DATA";
        return r;
    }

    @Override
    public SITM.SpeedReport[] runRange(int yf, int mf, int yt, int mt, Current current) {
        List<SITM.SpeedReport> all = new ArrayList<>();
        int y = yf, m = mf;
        while (y < yt || (y == yt && m <= mt)) {
            for (SITM.SpeedReport r : runMonth(y, m, current)) all.add(r);
            m++;
            if (m > 12) { m = 1; y++; }
        }
        return all.toArray(SITM.SpeedReport[]::new);
    }
}
