package edu.icesi.sitmmio.batchmaster.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class WorkerRegistry {
    private final List<SITM.IBatchWorkerPrx> workers = new CopyOnWriteArrayList<>();
    private final AtomicInteger rr = new AtomicInteger();

    public void register(SITM.IBatchWorkerPrx w) { if (w != null) workers.add(w); }
    public int size() { return workers.size(); }
    public List<SITM.IBatchWorkerPrx> snapshot() { return List.copyOf(workers); }

    public SITM.IBatchWorkerPrx pickRoundRobin() {
        if (workers.isEmpty()) return null;
        return workers.get(Math.abs(rr.getAndIncrement()) % workers.size());
    }
}
