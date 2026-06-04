package edu.icesi.sitmmio.batchworker;

import edu.icesi.sitmmio.batchworker.service.PartitionAggregator;
import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.GeoPoint;
import edu.icesi.sitmmio.domain.RouteMonthKey;
import edu.icesi.sitmmio.domain.SpeedResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartitionAggregatorTest {

    private Datagram mk(int busId, double lat, double lon, LocalDateTime t) {
        return new Datagram(0, "28-MAY-19", 1, 100L, new GeoPoint(lat, lon),
                (int)(lat*1e7), (int)(lon*1e7), 1, 131, 1L, 1L, t, busId);
    }

    @Test
    void computesNonZeroSpeedForCaliSegment() {
        RouteMonthKey key = new RouteMonthKey(131, YearMonth.of(2019, 5));
        Datagram a = mk(1069, 3.4516, -76.5320, LocalDateTime.of(2019, 5, 27, 10, 0, 0));
        Datagram b = mk(1069, 3.4761183, -76.4873683, LocalDateTime.of(2019, 5, 27, 10, 5, 0));
        SpeedResult r = new PartitionAggregator().aggregate(key, List.of(a, b), "T31", "Test");
        assertEquals("OK", r.status());
        assertEquals(131, r.lineId());
        assertTrue(r.averageSpeedKmH() > 0);
        assertEquals(1, r.validSegments());
    }

    @Test
    void emptyPartitionIsNoData() {
        SpeedResult r = new PartitionAggregator().aggregate(
                new RouteMonthKey(999, YearMonth.of(2019, 5)), List.of(), "X", "Y");
        assertEquals("NO_DATA", r.status());
    }
}
