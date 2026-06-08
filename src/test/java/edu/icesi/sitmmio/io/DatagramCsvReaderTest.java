package edu.icesi.sitmmio.io;

import edu.icesi.sitmmio.domain.Datagram;

import java.time.LocalDateTime;

public class DatagramCsvReaderTest {
    public static void run() {
        DatagramCsvReader reader = new DatagramCsvReader();
        Datagram datagram = reader.parseLine(
                "0,28-MAY-19,513327,70,34761183,-764873683,757,2241,159,6255401365,2019-05-27 20:14:43,1069"
        ).orElseThrow(() -> new AssertionError("Expected datagram to parse"));

        assertEquals(0, datagram.eventType(), "eventType");
        assertEquals(2241, datagram.lineId(), "lineId");
        assertEquals(159L, datagram.tripId(), "tripId");
        assertEquals(1069, datagram.busId(), "busId");
        assertEquals(LocalDateTime.of(2019, 5, 27, 20, 14, 43), datagram.datagramDate(), "datagramDate");
        assertClose(3.4761183, datagram.point().latitude(), 0.00000001, "latitude");
        assertClose(-76.4873683, datagram.point().longitude(), 0.00000001, "longitude");

        Datagram scientific = reader.parseLine(
                "0,28-MAY-19,513327,70,34761183,-764873683,757,2241,159,6.255401365E9,2019-05-27 20:14:43,1069"
        ).orElseThrow(() -> new AssertionError("Expected scientific notation to parse"));
        assertEquals(6255401365L, scientific.unknown1(), "unknown1 scientific notation");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but got " + actual);
        }
    }

    private static void assertClose(double expected, double actual, double tolerance, String label) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(label + " expected " + expected + " but got " + actual);
        }
    }
}
