package edu.icesi.sitmmio.batchworker.service;

import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.RouteMonthKey;
import edu.icesi.sitmmio.domain.SpeedResult;
import edu.icesi.sitmmio.service.DistanceCalculator;
import edu.icesi.sitmmio.service.SpeedAccumulator;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PartitionAggregator {

    private final DistanceCalculator distance = new DistanceCalculator();
    private final OutlierFilter outliers = OutlierFilter.defaults();

    public SpeedResult aggregate(RouteMonthKey key, List<Datagram> datagrams,
                                 String shortName, String description) {
        SpeedAccumulator acc = new SpeedAccumulator();

        Map<VehicleTripKey, List<Datagram>> byTrip = datagrams.stream()
                .filter(d -> d.lineId() == key.lineId())
                .filter(d -> d.lineId() != -1)
                .collect(Collectors.groupingBy(VehicleTripKey::from));

        for (List<Datagram> trail : byTrip.values()) {
            trail.sort(Comparator.comparing(Datagram::datagramDate));
            for (int i = 0; i < trail.size() - 1; i++) {
                Datagram a = trail.get(i), b = trail.get(i + 1);
                Duration dt = Duration.between(a.datagramDate(), b.datagramDate());
                double dist = distance.haversineKm(a.point(), b.point());
                double hours = dt.toMillis() / 3_600_000.0;
                double speed = hours > 0 ? dist / hours : Double.NaN;
                if (Double.isFinite(speed) && outliers.accept(speed, dist, dt)) {
                    acc.addSegment(dist, hours);
                } else {
                    acc.recordSkippedSegment();
                }
            }
        }

        double avg = acc.totalTimeHours() > 0 ? acc.totalDistanceKm() / acc.totalTimeHours() : 0.0;
        String status = acc.validSegments() > 0 ? "OK" : "NO_DATA";
        return new SpeedResult(
                key.lineId(), shortName, description, key.yearMonth(),
                acc.totalDistanceKm(), acc.totalTimeHours(), avg,
                acc.validSegments(), acc.skippedSegments(), status);
    }

    public AggregationSummary aggregateStreaming(RouteMonthKey key, Stream<Datagram> datagrams,
                                                 String shortName, String description) {
        SpeedAccumulator acc = new SpeedAccumulator();
        Map<VehicleTripKey, Datagram> previousByTrip = new HashMap<>();
        LongAdder datagramsRead = new LongAdder();

        datagrams
                .filter(d -> d.lineId() == key.lineId())
                .filter(d -> d.lineId() != -1)
                .forEachOrdered(current -> {
                    datagramsRead.increment();
                    VehicleTripKey tripKey = VehicleTripKey.from(current);
                    Datagram previous = previousByTrip.get(tripKey);
                    if (previous != null) {
                        Duration dt = Duration.between(previous.datagramDate(), current.datagramDate());
                        double hours = dt.toMillis() / 3_600_000.0;
                        double dist = distance.haversineKm(previous.point(), current.point());
                        double speed = hours > 0 ? dist / hours : Double.NaN;
                        if (Double.isFinite(speed) && outliers.accept(speed, dist, dt)) {
                            acc.addSegment(dist, hours);
                        } else {
                            acc.recordSkippedSegment();
                        }
                    }
                    previousByTrip.put(tripKey, current);
                });

        double avg = acc.totalTimeHours() > 0 ? acc.totalDistanceKm() / acc.totalTimeHours() : 0.0;
        String status = acc.validSegments() > 0 ? "OK" : "NO_DATA";
        SpeedResult result = new SpeedResult(
                key.lineId(), shortName, description, key.yearMonth(),
                acc.totalDistanceKm(), acc.totalTimeHours(), avg,
                acc.validSegments(), acc.skippedSegments(), status);
        return new AggregationSummary(result, datagramsRead.sum());
    }

    public record AggregationSummary(SpeedResult result, long datagramsRead) {
    }

    private record VehicleTripKey(int busId, int lineId, long tripId) {
        private static VehicleTripKey from(Datagram datagram) {
            return new VehicleTripKey(datagram.busId(), datagram.lineId(), datagram.tripId());
        }
    }
}
