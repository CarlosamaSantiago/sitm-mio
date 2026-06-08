package edu.icesi.sitmmio.analyticsstore;

import edu.icesi.sitmmio.analyticsstore.domain.ReportIndex;
import edu.icesi.sitmmio.domain.SpeedResult;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class ReportIndexTest {
    @Test
    void putAndGet() {
        ReportIndex idx = new ReportIndex();
        SpeedResult r = new SpeedResult(131, "T31", "x", YearMonth.of(2019, 5),
                100, 10, 10.0, 50, 5, "OK");
        idx.put(r);
        assertEquals(10.0, idx.get(131, 2019, 5).orElseThrow().averageSpeedKmH());
        assertTrue(idx.get(9999, 2019, 5).isEmpty());
        assertEquals(1, idx.getMonth(2019, 5).size());
    }
}
