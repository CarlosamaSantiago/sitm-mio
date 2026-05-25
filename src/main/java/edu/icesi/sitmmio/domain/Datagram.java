package edu.icesi.sitmmio.domain;

import java.time.LocalDateTime;

public record Datagram(
        int eventType,
        String registerDate,
        int stopId,
        long odometer,
        GeoPoint point,
        int rawLatitude,
        int rawLongitude,
        int taskId,
        int lineId,
        long tripId,
        long unknown1,
        LocalDateTime datagramDate,
        int busId
) {
}
