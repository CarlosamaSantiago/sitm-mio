package edu.icesi.sitmmio.service;

public class SpeedAccumulator {
    private double totalDistanceKm;
    private double totalTimeHours;
    private long validSegments;
    private long skippedSegments;

    public void addSegment(double distanceKm, double timeHours) {
        totalDistanceKm += distanceKm;
        totalTimeHours += timeHours;
        validSegments++;
    }

    public void recordSkippedSegment() {
        skippedSegments++;
    }

    public double totalDistanceKm() {
        return totalDistanceKm;
    }

    public double totalTimeHours() {
        return totalTimeHours;
    }

    public long validSegments() {
        return validSegments;
    }

    public long skippedSegments() {
        return skippedSegments;
    }
}
