package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.domain.GeoPoint;

public class DistanceCalculator {
    private static final double EARTH_RADIUS_KM = 6371.0088;

    public double haversineKm(GeoPoint start, GeoPoint end) {
        double startLat = Math.toRadians(start.latitude());
        double endLat = Math.toRadians(end.latitude());
        double deltaLat = Math.toRadians(end.latitude() - start.latitude());
        double deltaLon = Math.toRadians(end.longitude() - start.longitude());

        double a = Math.pow(Math.sin(deltaLat / 2.0), 2.0)
                + Math.cos(startLat) * Math.cos(endLat)
                * Math.pow(Math.sin(deltaLon / 2.0), 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_KM * c;
    }
}
