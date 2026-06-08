package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.Route;
import edu.icesi.sitmmio.domain.RouteMonthKey;
import edu.icesi.sitmmio.domain.SpeedResult;
import edu.icesi.sitmmio.io.DatagramCsvReader;
import edu.icesi.sitmmio.validation.DatagramValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class MonolithicSpeedCalculator {
    private static final long PROGRESS_INTERVAL_ROWS = 500_000L;
    private static final Duration MAX_SEGMENT_GAP = Duration.ofMinutes(5);
    private static final double MAX_SPEED_KMH = 120.0;

    private final DatagramCsvReader datagramCsvReader;
    private final DatagramValidator datagramValidator;
    private final DistanceCalculator distanceCalculator;

    public MonolithicSpeedCalculator() {
        this(new DatagramCsvReader(), new DatagramValidator(), new DistanceCalculator());
    }

    MonolithicSpeedCalculator(
            DatagramCsvReader datagramCsvReader,
            DatagramValidator datagramValidator,
            DistanceCalculator distanceCalculator
    ) {
        this.datagramCsvReader = datagramCsvReader;
        this.datagramValidator = datagramValidator;
        this.distanceCalculator = distanceCalculator;
    }

    public List<SpeedResult> calculate(
            Path datagramsPath,
            Map<Integer, Route> activeRoutes,
            MetricsCollector metrics
    ) throws IOException {
        Map<VehicleTripKey, Datagram> previousByTrip = new LinkedHashMap<>();
        Map<RouteMonthKey, SpeedAccumulator> accumulators = new LinkedHashMap<>();
        Set<YearMonth> observedMonths = new TreeSet<>();

        try (BufferedReader reader = Files.newBufferedReader(datagramsPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                metrics.recordProcessed();

                Optional<Datagram> parsed = datagramCsvReader.parseLine(line);
                if (parsed.isEmpty()) {
                    metrics.recordSkipped();
                    printProgress(metrics);
                    continue;
                }

                Datagram current = parsed.get();
                if (!datagramValidator.isValid(current, activeRoutes)) {
                    metrics.recordSkipped();
                    printProgress(metrics);
                    continue;
                }

                YearMonth yearMonth = YearMonth.from(current.datagramDate());
                observedMonths.add(yearMonth);

                VehicleTripKey tripKey = new VehicleTripKey(current.busId(), current.lineId(), current.tripId());
                Datagram previous = previousByTrip.get(tripKey);
                if (previous != null && !current.datagramDate().isAfter(previous.datagramDate())) {
                    metrics.recordSkipped();
                    incrementSkippedSegment(accumulators, current.lineId(), yearMonth);
                    printProgress(metrics);
                    continue;
                }

                metrics.recordValid();
                if (previous != null) {
                    processSegment(accumulators, previous, current, yearMonth);
                }
                previousByTrip.put(tripKey, current);

                printProgress(metrics);
            }
        }

        return buildResults(activeRoutes, observedMonths, accumulators);
    }

    private void processSegment(
            Map<RouteMonthKey, SpeedAccumulator> accumulators,
            Datagram previous,
            Datagram current,
            YearMonth currentMonth
    ) {
        RouteMonthKey key = new RouteMonthKey(current.lineId(), currentMonth);
        SpeedAccumulator accumulator = accumulators.computeIfAbsent(key, ignored -> new SpeedAccumulator());
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

    private void incrementSkippedSegment(
            Map<RouteMonthKey, SpeedAccumulator> accumulators,
            int lineId,
            YearMonth yearMonth
    ) {
        RouteMonthKey key = new RouteMonthKey(lineId, yearMonth);
        accumulators.computeIfAbsent(key, ignored -> new SpeedAccumulator()).recordSkippedSegment();
    }

    private List<SpeedResult> buildResults(
            Map<Integer, Route> activeRoutes,
            Set<YearMonth> observedMonths,
            Map<RouteMonthKey, SpeedAccumulator> accumulators
    ) {
        List<Route> routes = activeRoutes.values().stream()
                .sorted(Comparator.comparingInt(Route::lineId))
                .toList();
        List<SpeedResult> results = new ArrayList<>();

        for (YearMonth month : observedMonths) {
            for (Route route : routes) {
                RouteMonthKey key = new RouteMonthKey(route.lineId(), month);
                SpeedAccumulator accumulator = accumulators.getOrDefault(key, new SpeedAccumulator());
                boolean hasData = accumulator.validSegments() > 0L;
                double averageSpeed = hasData
                        ? accumulator.totalDistanceKm() / accumulator.totalTimeHours()
                        : 0.0;
                results.add(new SpeedResult(
                        route.lineId(),
                        route.shortName(),
                        route.description(),
                        month,
                        accumulator.totalDistanceKm(),
                        accumulator.totalTimeHours(),
                        averageSpeed,
                        accumulator.validSegments(),
                        accumulator.skippedSegments(),
                        hasData ? "OK" : "NO_DATA"
                ));
            }
        }

        return results;
    }

    private void printProgress(MetricsCollector metrics) {
        if (metrics.processedRows() % PROGRESS_INTERVAL_ROWS == 0L) {
            System.out.println("Processed rows: " + metrics.processedRows());
        }
    }

    private record VehicleTripKey(int busId, int lineId, long tripId) {
    }
}
