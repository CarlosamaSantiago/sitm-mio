package edu.icesi.sitmmio.batchworker.adapter;

import com.zeroc.Ice.Current;
import edu.icesi.sitmmio.batchworker.service.PartitionAggregator;
import edu.icesi.sitmmio.contracts.SliceMapper;
import edu.icesi.sitmmio.domain.Route;
import edu.icesi.sitmmio.domain.RouteMonthKey;
import edu.icesi.sitmmio.domain.SpeedResult;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
        edu.icesi.sitmmio.datalake.io.LakeReader reader =
                new edu.icesi.sitmmio.datalake.io.LakeReader(lakeRoot);

        Route route = routes.getOrDefault(key.lineId,
                new Route(key.lineId, "UNKNOWN", "NA"));
        PartitionAggregator.AggregationSummary summary;
        try (var stream = reader.streamPartition(rmk)) {
            summary = aggregator.aggregateStreaming(rmk, stream, route.shortName(), route.description());
        } catch (Exception e) {
            throw new SITM.NoDataForPartition();
        }

        SpeedResult result = summary.result();
        SITM.WorkerMetrics m = new SITM.WorkerMetrics();
        m.datagramsRead = summary.datagramsRead();
        m.validSegments = result.validSegments();
        m.outliersDropped = result.skippedSegments();
        m.minusOneFiltered = 0;
        m.elapsedMillis = System.currentTimeMillis() - start;
        lastMetrics.set(m);
        return SliceMapper.toSlice(result);
    }

    @Override
    public SITM.WorkerMetrics lastMetrics(Current current) { return lastMetrics.get(); }

    @Override
    public String workerId(Current current) { return workerId; }
}
