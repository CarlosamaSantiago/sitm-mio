package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.domain.GeoPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistanceCalculatorTest {

    private final DistanceCalculator calculator = new DistanceCalculator();

    @Test
    void caliSampleDistance() {
        double km = calculator.haversineKm(
                new GeoPoint(3.4516, -76.5320),
                new GeoPoint(3.4761183, -76.4873683)
        );
        assertEquals(5.66, km, 0.15, "Cali sample distance");
    }

    @Test
    void zeroDistanceWhenSamePoint() {
        double km = calculator.haversineKm(
                new GeoPoint(3.42, -76.52),
                new GeoPoint(3.42, -76.52)
        );
        assertEquals(0.0, km, 1e-9);
    }

    @Test
    void distanceIsSymmetric() {
        GeoPoint a = new GeoPoint(3.45, -76.55);
        GeoPoint b = new GeoPoint(3.50, -76.50);
        double ab = calculator.haversineKm(a, b);
        double ba = calculator.haversineKm(b, a);
        assertEquals(ab, ba, 1e-12);
    }

    @Test
    void distanceIsNonNegative() {
        double km = calculator.haversineKm(
                new GeoPoint(3.42, -76.52),
                new GeoPoint(-3.42, 76.52)
        );
        assertTrue(km >= 0.0);
    }
}
