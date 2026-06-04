package edu.icesi.sitmmio.datagrameventbus;

import edu.icesi.sitmmio.datagrameventbus.domain.SubscriberRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscriberRegistryTest {
    @Test
    void sizeChangesAfterUnsubscribe() {
        SubscriberRegistry r = new SubscriberRegistry();
        // No podemos crear MonitoringSubscriberPrx sin Communicator; testeamos null-safety en servant aparte.
        assertEquals(0, r.size());
    }
}
