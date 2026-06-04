package edu.icesi.sitmmio.datagramqueue.service;

import edu.icesi.sitmmio.contracts.SliceMapper;
import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.io.DatagramCsvReader;

import java.util.Optional;

/** CSV-line ↔ SITM.Datagram. Reusa DatagramCsvReader para deserializar. */
public final class DatagramSerializer {
    private static final DatagramCsvReader READER = new DatagramCsvReader();

    private DatagramSerializer() {}

    public static String toCsv(SITM.Datagram d) {
        return d.eventType + "," + d.registerDate + "," + d.stopId + "," + d.odometer
                + "," + d.latitude + "," + d.longitude + "," + d.taskId + "," + d.lineId
                + "," + d.tripId + "," + d.unknown1 + "," + d.datagramDate + "," + d.busId;
    }

    public static Optional<SITM.Datagram> fromCsv(String line) {
        return READER.parseLine(line).map(SliceMapper::toSlice);
    }
}
