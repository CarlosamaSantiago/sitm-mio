package edu.icesi.sitmmio.validation;

import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.GeoPoint;
import edu.icesi.sitmmio.domain.Route;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatagramValidatorTest {

    private final DatagramValidator validator = new DatagramValidator();
    private final Map<Integer, Route> activeRoutes = Map.of(
            131, new Route(131, "T31", "Terminal Paso del Comercio - Universidades"),
            2241, new Route(2241, "TEST", "Test route")
    );

    private Datagram baseDatagram() {
        return new Datagram(
                0, "28-MAY-19", 513327, 70L,
                new GeoPoint(3.4761183, -76.4873683),
                34761183, -764873683,
                757, 2241, 159L, 6255401365L,
                LocalDateTime.of(2019, 5, 27, 20, 14, 43),
                1069
        );
    }

    @Test
    void validDatagramPasses() {
        assertTrue(validator.isValid(baseDatagram(), activeRoutes));
    }

    @Test
    void rejectsRawLatitudeMinusOne() {
        Datagram b = baseDatagram();
        Datagram d = new Datagram(b.eventType(), b.registerDate(), b.stopId(), b.odometer(),
                b.point(), -1, b.rawLongitude(),
                b.taskId(), b.lineId(), b.tripId(), b.unknown1(), b.datagramDate(), b.busId());
        assertFalse(validator.isValid(d, activeRoutes));
    }

    @Test
    void rejectsRawLongitudeMinusOne() {
        Datagram b = baseDatagram();
        Datagram d = new Datagram(b.eventType(), b.registerDate(), b.stopId(), b.odometer(),
                b.point(), b.rawLatitude(), -1,
                b.taskId(), b.lineId(), b.tripId(), b.unknown1(), b.datagramDate(), b.busId());
        assertFalse(validator.isValid(d, activeRoutes));
    }

    @Test
    void rejectsNonPositiveBusId() {
        Datagram b = baseDatagram();
        Datagram d = new Datagram(b.eventType(), b.registerDate(), b.stopId(), b.odometer(),
                b.point(), b.rawLatitude(), b.rawLongitude(),
                b.taskId(), b.lineId(), b.tripId(), b.unknown1(), b.datagramDate(), 0);
        assertFalse(validator.isValid(d, activeRoutes));
    }

    @Test
    void rejectsNegativeTripId() {
        Datagram b = baseDatagram();
        Datagram d = new Datagram(b.eventType(), b.registerDate(), b.stopId(), b.odometer(),
                b.point(), b.rawLatitude(), b.rawLongitude(),
                b.taskId(), b.lineId(), -1L, b.unknown1(), b.datagramDate(), b.busId());
        assertFalse(validator.isValid(d, activeRoutes));
    }

    @Test
    void rejectsUnknownLineId() {
        Datagram b = baseDatagram();
        Datagram d = new Datagram(b.eventType(), b.registerDate(), b.stopId(), b.odometer(),
                b.point(), b.rawLatitude(), b.rawLongitude(),
                b.taskId(), 99999, b.tripId(), b.unknown1(), b.datagramDate(), b.busId());
        assertFalse(validator.isValid(d, activeRoutes));
    }
}
