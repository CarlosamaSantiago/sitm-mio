package edu.icesi.sitmmio.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConcurrentPartitionerTest {
    @Test
    void sameVehicleTripKeyAlwaysMapsToSameWorker() {
        ConcurrentPartitioner partitioner = new ConcurrentPartitioner();

        int first = partitioner.partition(1069, 131, 77L, 8);
        int second = partitioner.partition(1069, 131, 77L, 8);

        assertEquals(first, second);
    }

    @Test
    void rejectsNonPositiveWorkerCount() {
        ConcurrentPartitioner partitioner = new ConcurrentPartitioner();

        assertThrows(IllegalArgumentException.class,
                () -> partitioner.partition(1069, 131, 77L, 0));
    }
}
