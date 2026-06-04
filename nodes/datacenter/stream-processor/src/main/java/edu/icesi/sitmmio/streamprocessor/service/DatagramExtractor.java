package edu.icesi.sitmmio.streamprocessor.service;

import edu.icesi.sitmmio.contracts.SliceMapper;
import edu.icesi.sitmmio.domain.Datagram;

/** R12: extrae la tupla (busId, lat, lon, ts) normalizada. */
public final class DatagramExtractor {
    public Datagram extract(SITM.Datagram d) { return SliceMapper.toRecord(d); }
}
