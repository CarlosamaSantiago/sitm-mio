package edu.icesi.sitmmio.datalake;

import edu.icesi.sitmmio.datalake.io.FileLakeStore;
import edu.icesi.sitmmio.datalake.io.LakeReader;
import edu.icesi.sitmmio.domain.RouteMonthKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LakeRoundtripTest {

    private SITM.Datagram makeDatagram(int lineId, int year, int month, int day, int busId) {
        SITM.Datagram d = new SITM.Datagram();
        d.eventType = 0; d.registerDate = "28-MAY-19"; d.stopId = 1; d.odometer = 100;
        d.latitude = 34761183; d.longitude = -764873683;
        d.taskId = 1; d.lineId = lineId; d.tripId = 1; d.unknown1 = 1;
        d.datagramDate = String.format("%04d-%02d-%02d 10:00:00", year, month, day);
        d.busId = busId;
        return d;
    }

    @Test
    void archiveAndStreamPartition(@TempDir Path tmp) throws Exception {
        FileLakeStore store = new FileLakeStore(tmp);
        store.archive(makeDatagram(131, 2019, 5, 27, 1069));
        store.archive(makeDatagram(131, 2019, 5, 27, 1070));
        store.archive(makeDatagram(140, 2019, 5, 27, 2000));
        store.flush();
        store.close();

        LakeReader reader = new LakeReader(tmp);
        List<RouteMonthKey> partitions = reader.listPartitions();
        assertTrue(partitions.contains(new RouteMonthKey(131, YearMonth.of(2019, 5))));
        assertTrue(partitions.contains(new RouteMonthKey(140, YearMonth.of(2019, 5))));

        long count131 = reader.streamPartition(new RouteMonthKey(131, YearMonth.of(2019, 5))).count();
        assertEquals(2, count131);
    }
}
