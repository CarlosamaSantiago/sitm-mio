package edu.icesi.sitmmio.contracts;

import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.GeoPoint;
import edu.icesi.sitmmio.domain.RouteMonthKey;
import edu.icesi.sitmmio.domain.SpeedResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SliceMapperTest {

    @Test
    void datagramRoundtripIsIdentity() {
        Datagram original = new Datagram(
                0, "28-MAY-19", 513327, 70L,
                new GeoPoint(3.4761183, -76.4873683),
                34761183, -764873683,
                757, 2241, 159L, 6255401365L,
                LocalDateTime.of(2019, 5, 27, 20, 14, 43),
                1069
        );

        Datagram back = SliceMapper.toRecord(SliceMapper.toSlice(original));

        assertEquals(original.eventType(), back.eventType());
        assertEquals(original.registerDate(), back.registerDate());
        assertEquals(original.stopId(), back.stopId());
        assertEquals(original.rawLatitude(), back.rawLatitude());
        assertEquals(original.rawLongitude(), back.rawLongitude());
        assertEquals(original.taskId(), back.taskId());
        assertEquals(original.lineId(), back.lineId());
        assertEquals(original.busId(), back.busId());
        assertEquals(original.datagramDate(), back.datagramDate());
        assertEquals(original.point().latitude(), back.point().latitude(), 1e-9);
        assertEquals(original.point().longitude(), back.point().longitude(), 1e-9);
    }

    @Test
    void speedReportRoundtripIsIdentity() {
        SpeedResult original = new SpeedResult(
                131, "T31", "Terminal Paso del Comercio - Universidades",
                YearMonth.of(2019, 5),
                19066.452520, 1097.321666667, 17.375445,
                206566L, 5871L, "OK"
        );

        SpeedResult back = SliceMapper.toRecord(SliceMapper.toSlice(original));

        assertEquals(original.lineId(), back.lineId());
        assertEquals(original.shortName(), back.shortName());
        assertEquals(original.description(), back.description());
        assertEquals(original.yearMonth(), back.yearMonth());
        assertEquals(original.totalDistanceKm(), back.totalDistanceKm(), 1e-9);
        assertEquals(original.totalTimeHours(), back.totalTimeHours(), 1e-9);
        assertEquals(original.averageSpeedKmH(), back.averageSpeedKmH(), 1e-9);
        assertEquals(original.validSegments(), back.validSegments());
        assertEquals(original.skippedSegments(), back.skippedSegments());
        assertEquals(original.status(), back.status());
    }

    @Test
    void partitionKeyRoundtripIsIdentity() {
        RouteMonthKey original = new RouteMonthKey(131, YearMonth.of(2019, 5));
        RouteMonthKey back = SliceMapper.toRecord(SliceMapper.toSlice(original));
        assertEquals(original, back);
    }

    @Test
    void latitudeDescalingMatchesDictionary() {
        // Diccionario: latitude entero 34761183 → 3.4761183°
        SITM.Datagram slice = new SITM.Datagram();
        slice.latitude = 34761183;
        slice.longitude = -764873683;
        slice.datagramDate = "2019-05-27 20:14:43";
        slice.registerDate = "28-MAY-19";
        Datagram record = SliceMapper.toRecord(slice);
        assertEquals(3.4761183, record.point().latitude(), 1e-9);
        assertEquals(-76.4873683, record.point().longitude(), 1e-9);
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> SliceMapper.toRecord((SITM.Datagram) null));
        assertThrows(NullPointerException.class, () -> SliceMapper.toSlice((Datagram) null));
    }
}
