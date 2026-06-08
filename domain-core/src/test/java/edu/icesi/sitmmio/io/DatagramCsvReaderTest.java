package edu.icesi.sitmmio.io;

import edu.icesi.sitmmio.domain.Datagram;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatagramCsvReaderTest {

    private final DatagramCsvReader reader = new DatagramCsvReader();

    @Test
    void parsesCanonicalLine() {
        Datagram d = reader.parseLine(
                "0,28-MAY-19,513327,70,34761183,-764873683,757,2241,159,6255401365,2019-05-27 20:14:43,1069"
        ).orElseThrow();

        assertEquals(0, d.eventType());
        assertEquals(2241, d.lineId());
        assertEquals(159L, d.tripId());
        assertEquals(1069, d.busId());
        assertEquals(LocalDateTime.of(2019, 5, 27, 20, 14, 43), d.datagramDate());
        assertEquals(3.4761183, d.point().latitude(), 1e-8);
        assertEquals(-76.4873683, d.point().longitude(), 1e-8);
    }

    @Test
    void parsesUnknown1InScientificNotation() {
        Optional<Datagram> parsed = reader.parseLine(
                "0,28-MAY-19,513327,70,34761183,-764873683,757,2241,159,6.255401365E9,2019-05-27 20:14:43,1069"
        );
        assertTrue(parsed.isPresent(), "should parse scientific notation in unknown1");
        assertEquals(2241, parsed.get().lineId());
    }

    @Test
    void skipsMalformedLine() {
        Optional<Datagram> parsed = reader.parseLine("not,enough,fields");
        assertTrue(parsed.isEmpty(), "malformed line should not parse");
    }
}
