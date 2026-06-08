package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.concurrent.ConcurrentCalculationResult;
import edu.icesi.sitmmio.domain.Route;
import edu.icesi.sitmmio.domain.SpeedResult;
import edu.icesi.sitmmio.io.RouteCsvReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentSpeedCalculatorTest {
    @TempDir
    Path tempDir;

    @Test
    void workerCountOneAndManyProduceSameResults() throws Exception {
        Fixture fixture = writeFixture();

        ConcurrentCalculationResult one = new ConcurrentSpeedCalculator(1)
                .calculate(fixture.datagramsPath(), fixture.routes());
        ConcurrentCalculationResult four = new ConcurrentSpeedCalculator(4)
                .calculate(fixture.datagramsPath(), fixture.routes());

        assertSameResults(one.results(), four.results());
        assertEquals(6, four.metrics().processedRows());
        assertEquals(4, four.metrics().validRows());
        assertEquals(2, four.metrics().skippedRows());
        assertEquals(4, four.metrics().workerCount());
    }

    @Test
    void includesActiveRoutesWithoutSegmentsAsNoData() throws Exception {
        Fixture fixture = writeFixture();

        List<SpeedResult> results = new ConcurrentSpeedCalculator(2)
                .calculate(fixture.datagramsPath(), fixture.routes())
                .results();

        SpeedResult route150 = find(results, 150);
        assertEquals(YearMonth.of(2019, 5), route150.yearMonth());
        assertEquals("NO_DATA", route150.status());
        assertEquals(0, route150.validSegments());
    }

    @Test
    void aggregatesExpectedSegmentsAndSkippedSegments() throws Exception {
        Fixture fixture = writeFixture();

        List<SpeedResult> results = new ConcurrentSpeedCalculator(3)
                .calculate(fixture.datagramsPath(), fixture.routes())
                .results();

        SpeedResult route131 = find(results, 131);
        assertEquals("OK", route131.status());
        assertEquals(2, route131.validSegments());
        assertEquals(1, route131.skippedSegments());
        assertTrue(route131.averageSpeedKmH() > 0.0);
    }

    private Fixture writeFixture() throws Exception {
        Path routesPath = tempDir.resolve("routes.csv");
        Files.writeString(routesPath, String.join(System.lineSeparator(),
                "LINEID,PLANVERSIONID,SHORTNAME,DESCRIPTION",
                "131,1,T31,Route 31",
                "140,1,T40,Route 40",
                "150,1,T50,Route 50") + System.lineSeparator());

        Path datagramsPath = tempDir.resolve("datagrams.csv");
        Files.writeString(datagramsPath, String.join(System.lineSeparator(),
                row(131, 77, "2019-05-27 10:00:00", 30000000, -760000000, 10),
                row(131, 77, "2019-05-27 10:01:00", 30001000, -760000000, 10),
                row(131, 77, "2019-05-27 10:02:00", 30002000, -760000000, 10),
                row(131, 77, "2019-05-27 09:59:00", 30003000, -760000000, 10),
                row(999, 1, "2019-05-27 10:03:00", 30002000, -760000000, 10),
                row(140, 2, "2019-05-27 10:00:00", 30000000, -760000000, 11)) + System.lineSeparator());

        Map<Integer, Route> routes = new RouteCsvReader().readActiveRoutes(routesPath);
        return new Fixture(datagramsPath, routes);
    }

    private String row(int lineId, long tripId, String date, int rawLat, int rawLon, int busId) {
        return "0,28-MAY-19,513327,70," + rawLat + "," + rawLon
                + ",757," + lineId + "," + tripId + ",6255401365," + date + "," + busId;
    }

    private SpeedResult find(List<SpeedResult> results, int lineId) {
        return results.stream()
                .filter(result -> result.lineId() == lineId)
                .findFirst()
                .orElseThrow();
    }

    private void assertSameResults(List<SpeedResult> expected, List<SpeedResult> actual) {
        List<SpeedResult> left = expected.stream()
                .sorted(Comparator.comparing(SpeedResult::yearMonth).thenComparingInt(SpeedResult::lineId))
                .toList();
        List<SpeedResult> right = actual.stream()
                .sorted(Comparator.comparing(SpeedResult::yearMonth).thenComparingInt(SpeedResult::lineId))
                .toList();
        assertEquals(left.size(), right.size());
        for (int i = 0; i < left.size(); i++) {
            SpeedResult a = left.get(i);
            SpeedResult b = right.get(i);
            assertEquals(a.lineId(), b.lineId());
            assertEquals(a.yearMonth(), b.yearMonth());
            assertEquals(a.validSegments(), b.validSegments());
            assertEquals(a.skippedSegments(), b.skippedSegments());
            assertEquals(a.totalDistanceKm(), b.totalDistanceKm(), 1e-9);
            assertEquals(a.totalTimeHours(), b.totalTimeHours(), 1e-9);
            assertEquals(a.averageSpeedKmH(), b.averageSpeedKmH(), 1e-9);
            assertEquals(a.status(), b.status());
        }
    }

    private record Fixture(Path datagramsPath, Map<Integer, Route> routes) {
    }
}
