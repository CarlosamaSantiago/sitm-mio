package edu.icesi.sitmmio.streamprocessor;

import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.GeoPoint;
import edu.icesi.sitmmio.streamprocessor.service.EventClassifier;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassifierTest {
    private Datagram mk(int eventType) {
        return new Datagram(eventType, "28-MAY-19", 1, 100L, new GeoPoint(3.42, -76.52),
                34000000, -765000000, 1, 131, 1L, 1L,
                LocalDateTime.of(2019, 5, 27, 10, 0), 1069);
    }

    @Test
    void routineForType0() {
        assertEquals(EventClassifier.Category.ROUTINE, new EventClassifier().classify(mk(0)));
    }
    @Test
    void exceptionalForOther() {
        assertEquals(EventClassifier.Category.EXCEPTIONAL, new EventClassifier().classify(mk(50)));
    }
}
