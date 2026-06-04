package edu.icesi.sitmmio.contracts;

import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.GeoPoint;
import edu.icesi.sitmmio.domain.GpsConstants;
import edu.icesi.sitmmio.domain.RouteMonthKey;
import edu.icesi.sitmmio.domain.SpeedResult;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Adaptador puro Java entre los DTOs Slice de {@link SITM} y los records de dominio
 * en {@code edu.icesi.sitmmio.domain}. Conserva la semántica del Diccionario:
 * lat/lon enteros x10^7, fecha "YYYY-MM-DD HH:MM:SS".
 *
 * Esta clase es la única frontera Slice ↔ dominio; los nodos NUNCA deben construir
 * structs Slice a mano: siempre pasan por aquí.
 */
public final class SliceMapper {

    private static final DateTimeFormatter DATAGRAM_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SliceMapper() {}

    // ───────────────────────── Datagram ─────────────────────────

    public static Datagram toRecord(SITM.Datagram slice) {
        Objects.requireNonNull(slice, "slice datagram");
        int rawLat = slice.latitude;
        int rawLon = slice.longitude;
        GeoPoint point = new GeoPoint(
                rawLat / (double) GpsConstants.COORD_SCALE,
                rawLon / (double) GpsConstants.COORD_SCALE
        );
        LocalDateTime when = LocalDateTime.parse(slice.datagramDate, DATAGRAM_DATE_FORMAT);
        return new Datagram(
                slice.eventType,
                slice.registerDate,
                slice.stopId,
                slice.odometer,
                point,
                rawLat,
                rawLon,
                slice.taskId,
                slice.lineId,
                slice.tripId,
                slice.unknown1,
                when,
                slice.busId
        );
    }

    public static SITM.Datagram toSlice(Datagram record) {
        Objects.requireNonNull(record, "domain datagram");
        SITM.Datagram d = new SITM.Datagram();
        d.eventType = record.eventType();
        d.registerDate = record.registerDate();
        d.stopId = record.stopId();
        d.odometer = (int) record.odometer();
        d.latitude = record.rawLatitude();
        d.longitude = record.rawLongitude();
        d.taskId = record.taskId();
        d.lineId = record.lineId();
        d.tripId = (int) record.tripId();
        d.unknown1 = (int) record.unknown1();
        d.datagramDate = record.datagramDate().format(DATAGRAM_DATE_FORMAT);
        d.busId = record.busId();
        return d;
    }

    // ───────────────────────── SpeedReport ─────────────────────────

    public static SITM.SpeedReport toSlice(SpeedResult result) {
        Objects.requireNonNull(result, "speed result");
        SITM.SpeedReport r = new SITM.SpeedReport();
        r.lineId = result.lineId();
        r.shortName = result.shortName();
        r.description = result.description();
        r.year = result.yearMonth().getYear();
        r.month = result.yearMonth().getMonthValue();
        r.totalDistanceKm = result.totalDistanceKm();
        r.totalTimeHours = result.totalTimeHours();
        r.averageSpeedKmH = result.averageSpeedKmH();
        r.validSegments = result.validSegments();
        r.skippedSegments = result.skippedSegments();
        r.status = result.status();
        return r;
    }

    public static SpeedResult toRecord(SITM.SpeedReport report) {
        Objects.requireNonNull(report, "slice speed report");
        return new SpeedResult(
                report.lineId,
                report.shortName,
                report.description,
                YearMonth.of(report.year, report.month),
                report.totalDistanceKm,
                report.totalTimeHours,
                report.averageSpeedKmH,
                report.validSegments,
                report.skippedSegments,
                report.status
        );
    }

    // ───────────────────────── PartitionKey ─────────────────────────

    public static SITM.PartitionKey toSlice(RouteMonthKey key) {
        Objects.requireNonNull(key, "partition key");
        SITM.PartitionKey k = new SITM.PartitionKey();
        k.lineId = key.lineId();
        k.year = key.yearMonth().getYear();
        k.month = key.yearMonth().getMonthValue();
        return k;
    }

    public static RouteMonthKey toRecord(SITM.PartitionKey key) {
        Objects.requireNonNull(key, "slice partition key");
        return new RouteMonthKey(key.lineId, YearMonth.of(key.year, key.month));
    }
}
