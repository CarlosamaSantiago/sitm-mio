package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.domain.Datagram;

public final class ConcurrentPartitioner {
    public int partition(Datagram datagram, int workerCount) {
        return partition(datagram.busId(), datagram.lineId(), datagram.tripId(), workerCount);
    }

    public int partition(int busId, int lineId, long tripId, int workerCount) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("workerCount must be positive");
        }
        int hash = 17;
        hash = 31 * hash + busId;
        hash = 31 * hash + lineId;
        hash = 31 * hash + Long.hashCode(tripId);
        return Math.floorMod(hash, workerCount);
    }
}
