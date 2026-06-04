package edu.icesi.sitmmio.batchworker.adapter;

import com.zeroc.Ice.Current;
import edu.icesi.sitmmio.batchworker.service.PartitionAggregator;
import edu.icesi.sitmmio.contracts.SliceMapper;
import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.Route;
import edu.icesi.sitmmio.domain.RouteMonthKey;
import edu.icesi.sitmmio.domain.SpeedResult;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BatchWorkerI implements SITM.IBatchWorker {

    private final String workerId;
    private final Path lakeRoot;
    private final Map<Integer, Route> routes;
    private final PartitionAggregator aggregator = new PartitionAggregator();
    private final AtomicReference<SITM.WorkerMetrics> lastMetrics =
            new AtomicReference<>(new SITM.WorkerMetrics());

    public BatchWorkerI(String workerId, Path lakeRoot, Map<Integer, Route> routes) {
        this.workerId = workerId;
        this.lakeRoot = lakeRoot;
        this.routes = routes;
    }

    @Override
    public SITM.SpeedReport computePartition(SITM.PartitionKey key, Current current)
            throws SITM.NoDataForPartition {
        long start = System.currentTimeMillis();
        RouteMonthKey rmk = SliceMapper.toRecord(key);

        // Lectura directa del DataLake usando la misma lógica que LakeReader
        edu.icesi.sitmmio.datalake.io.LakeReader reader =
                new edu.icesi.sitmmio.datalake.io.LakeReader(lakeRoot);
        List<Datagram> data;
        try (var stream = reader.streamPartition(rmk)) {
            data = stream.collect(Collectors.toList());
        } catch (Exception e) {
            throw new SITM.NoDataForPartition();
        }

        Route route = routes.getOrDefault(key.lineId,
                new Route(key.lineId, "UNKNOWN", "NA"));
        SpeedResult result = aggregator.aggregate(rmk, data, route.shortName(), route.description());

        if ("NO_DATA".equals(result.status()) && data.isEmpty()) {
            // Mantener simetría con monolith: emit NO_DATA en vez de excepción
        }

        SITM.WorkerMetrics m = new SITM.WorkerMetrics();
        m.datagramsRead = data.size();
        m.validSegments = result.validSegments();
        m.outliersDropped = result.skippedSegments();
        m.minusOneFiltered = data.stream().filter(d -> d.lineId() == -1).count();
        m.elapsedMillis = System.currentTimeMillis() - start;
        lastMetrics.set(m);
        return SliceMapper.toSlice(result);
    }

    @Override
    public SITM.WorkerMetrics lastMetrics(Current current) { return lastMetrics.get(); }

    @Override
    public String workerId(Current current) { return workerId; }
}
