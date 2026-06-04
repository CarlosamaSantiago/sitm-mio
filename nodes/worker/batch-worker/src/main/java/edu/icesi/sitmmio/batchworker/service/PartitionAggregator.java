package edu.icesi.sitmmio.batchworker.service;

import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.RouteMonthKey;
import edu.icesi.sitmmio.domain.SpeedResult;
import edu.icesi.sitmmio.service.DistanceCalculator;
import edu.icesi.sitmmio.service.SpeedAccumulator;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public final class PartitionAggregator {

    private final DistanceCalculator distance = new DistanceCalculator();
    private final OutlierFilter outliers = OutlierFilter.defaults();

    public SpeedResult aggregate(RouteMonthKey key, List<Datagram> datagrams,
                                 String shortName, String description) {
        SpeedAccumulator acc = new SpeedAccumulator();

        Map<Integer, List<Datagram>> byBus = datagrams.stream()
                .filter(d -> d.lineId() == key.lineId())
                .filter(d -> d.lineId() != -1)
                .collect(Collectors.groupingBy(Datagram::busId));

        for (List<Datagram> trail : byBus.values()) {
            trail.sort(Comparator.comparing(Datagram::datagramDate));
            for (int i = 0; i < trail.size() - 1; i++) {
                Datagram a = trail.get(i), b = trail.get(i + 1);
                Duration dt = Duration.between(a.datagramDate(), b.datagramDate());
                double dist = distance.haversineKm(a.point(), b.point());
                double hours = dt.getSeconds() / 3600.0;
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
}
