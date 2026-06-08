package edu.icesi.sitmmio.datalake.service;

import edu.icesi.sitmmio.contracts.SliceMapper;
import edu.icesi.sitmmio.domain.Datagram;

public final class Partitioner {
    public static int[] keyOf(SITM.Datagram d) {
        Datagram r = SliceMapper.toRecord(d);
        return new int[]{ r.lineId(), r.datagramDate().getYear(), r.datagramDate().getMonthValue() };
    }
}
