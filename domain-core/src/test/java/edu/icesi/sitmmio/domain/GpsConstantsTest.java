package edu.icesi.sitmmio.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GpsConstantsTest {
    @Test void coordScale()    { assertEquals(10_000_000, GpsConstants.COORD_SCALE); }
    @Test void earthRadius()   { assertEquals(6371.0088, GpsConstants.EARTH_RADIUS_KM, 1e-12); }
    @Test void nullSentinel()  { assertEquals(-1, GpsConstants.NULL_SENTINEL); }

    @Test
    void rawCoordinateDescalesCorrectly() {
        double lat = 34761183 / (double) GpsConstants.COORD_SCALE;
        double lon = -764873683 / (double) GpsConstants.COORD_SCALE;
        assertEquals(3.4761183, lat, 1e-8);
        assertEquals(-76.4873683, lon, 1e-8);
    }
}
