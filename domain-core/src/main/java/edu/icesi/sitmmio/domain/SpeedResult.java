package edu.icesi.sitmmio.domain;

import java.time.YearMonth;

public record SpeedResult(
        int lineId,
        String shortName,
        String description,
        YearMonth yearMonth,
        double totalDistanceKm,
        double totalTimeHours,
        double averageSpeedKmH,
        long validSegments,
        long skippedSegments,
        String status
) {
}
