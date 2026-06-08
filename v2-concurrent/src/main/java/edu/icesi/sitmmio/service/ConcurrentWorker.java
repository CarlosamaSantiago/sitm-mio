package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.RouteMonthKey;

import java.time.Duration;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

final class ConcurrentWorker implements Callable<WorkerResult> {
    private static final Duration MAX_SEGMENT_GAP = Duration.ofMinutes(5);
    private static final double MAX_SPEED_KMH = 120.0;

    private final BlockingQueue<WorkItem> queue;
    private final DistanceCalculator distanceCalculator;
    private final Map<VehicleTripKey, Datagram> previousByTrip = new LinkedHashMap<>();
    private final Map<RouteMonthKey, SpeedTotals> accumulators = new LinkedHashMap<>();
    private long validRows;
    private long skippedRows;

    ConcurrentWorker(BlockingQueue<WorkItem> queue) {
        this(queue, new DistanceCalculator());
    }

    ConcurrentWorker(BlockingQueue<WorkItem> queue, DistanceCalculator distanceCalculator) {
        this.queue = queue;
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public WorkerResult call() throws InterruptedException {
        while (true) {
            WorkItem item = queue.take();
            if (item.poison()) {
                return new WorkerResult(new LinkedHashMap<>(accumulators), validRows, skippedRows);
            }
            process(item.datagram());
        }
    }

    private void process(Datagram current) {
        YearMonth yearMonth = YearMonth.from(current.datagramDate());
        VehicleTripKey tripKey = new VehicleTripKey(current.busId(), current.lineId(), current.tripId());
        Datagram previous = previousByTrip.get(tripKey);
        if (previous != null && !current.datagramDate().isAfter(previous.datagramDate())) {
            skippedRows++;
            accumulator(current.lineId(), yearMonth).recordSkippedSegment();
            return;
        }

        validRows++;
        if (previous != null) {
            processSegment(previous, current, yearMonth);
        }
        previousByTrip.put(tripKey, current);
    }

    private void processSegment(Datagram previous, Datagram current, YearMonth currentMonth) {
        SpeedTotals accumulator = accumulator(current.lineId(), currentMonth);
        Duration duration = Duration.between(previous.datagramDate(), current.datagramDate());
        double timeHours = duration.toMillis() / 3_600_000.0;

        if (timeHours <= 0.0 || duration.compareTo(MAX_SEGMENT_GAP) > 0) {
            accumulator.recordSkippedSegment();
            return;
        }

        double distanceKm = distanceCalculator.haversineKm(previous.point(), current.point());
        double speedKmH = distanceKm / timeHours;
        if (speedKmH > MAX_SPEED_KMH) {
            accumulator.recordSkippedSegment();
            return;
        }

        accumulator.addSegment(distanceKm, timeHours);
    }

    private SpeedTotals accumulator(int lineId, YearMonth yearMonth) {
        RouteMonthKey key = new RouteMonthKey(lineId, yearMonth);
        return accumulators.computeIfAbsent(key, ignored -> new SpeedTotals());
    }

    private record VehicleTripKey(int busId, int lineId, long tripId) {
    }
}
