package edu.icesi.sitmmio.batchmaster;

import edu.icesi.sitmmio.batchmaster.service.Partitioner;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartitionerTest {
    @Test
    void partitionsForMonth() {
        Set<Integer> lines = Set.of(131, 140, 142);
        var keys = new Partitioner().forMonth(lines, 2019, 5);
        assertEquals(3, keys.size());
        for (var k : keys) {
            assertEquals(2019, k.year);
            assertEquals(5, k.month);
        }
    }
}
