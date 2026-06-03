package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.domain.GeoPoint;

public class DistanceCalculatorTest {
    public static void run() {
        DistanceCalculator calculator = new DistanceCalculator();
        double distance = calculator.haversineKm(
                new GeoPoint(3.4516, -76.5320),
                new GeoPoint(3.4761183, -76.4873683)
        );

        assertClose(distance, 5.66, 0.15, "Cali sample distance");
    }

    private static void assertClose(double actual, double expected, double tolerance, String label) {
        if (Math.abs(actual - expected) > tolerance) {
            throw new AssertionError(label + " expected " + expected + " but got " + actual);
        }
    }
}
