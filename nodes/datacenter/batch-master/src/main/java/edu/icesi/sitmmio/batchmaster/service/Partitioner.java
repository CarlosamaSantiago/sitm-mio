package edu.icesi.sitmmio.batchmaster.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Partitioner {
    public List<SITM.PartitionKey> forMonth(Set<Integer> activeLineIds, int year, int month) {
        List<SITM.PartitionKey> keys = new ArrayList<>();
        for (int lineId : activeLineIds) {
            SITM.PartitionKey k = new SITM.PartitionKey();
            k.lineId = lineId; k.year = year; k.month = month;
            keys.add(k);
        }
        return keys;
    }
}
