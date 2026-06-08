package edu.icesi.sitmmio.service;

final class SpeedTotals {
    private double totalDistanceKm;
    private double totalTimeHours;
    private long validSegments;
    private long skippedSegments;

    void addSegment(double distanceKm, double timeHours) {
        totalDistanceKm += distanceKm;
        totalTimeHours += timeHours;
        validSegments++;
    }

    void recordSkippedSegment() {
        skippedSegments++;
    }

    void merge(SpeedTotals other) {
        totalDistanceKm += other.totalDistanceKm;
        totalTimeHours += other.totalTimeHours;
        validSegments += other.validSegments;
        skippedSegments += other.skippedSegments;
    }

    double totalDistanceKm() {
        return totalDistanceKm;
    }

    double totalTimeHours() {
        return totalTimeHours;
    }

    long validSegments() {
        return validSegments;
    }

    long skippedSegments() {
        return skippedSegments;
    }
}
