package edu.icesi.sitmmio.io;

import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.GeoPoint;
import edu.icesi.sitmmio.domain.GpsConstants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parser CSV del datagrama. Heredado del monolith con dos extensiones para V3:
 *  - Soporte de notación científica en columnas numéricas (`unknown1`, `tripId`)
 *    para compatibilidad con datasets reales (ej. `6.255401365E9`).
 *  - Uso de {@link GpsConstants#COORD_SCALE} centralizado.
 */
public class DatagramCsvReader {
    public static final int EXPECTED_COLUMNS = 12;
    private static final double COORDINATE_SCALE = (double) GpsConstants.COORD_SCALE;
    private static final DateTimeFormatter DATAGRAM_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Optional<Datagram> parseLine(String line) {
        String[] columns = splitCsvLine(line);
        if (columns.length != EXPECTED_COLUMNS) {
            return Optional.empty();
        }

        try {
            int rawLatitude = Integer.parseInt(columns[4].trim());
            int rawLongitude = Integer.parseInt(columns[5].trim());
            LocalDateTime datagramDate = LocalDateTime.parse(columns[10].trim(), DATAGRAM_DATE_FORMAT);

            return Optional.of(new Datagram(
                    Integer.parseInt(columns[0].trim()),
                    columns[1].trim(),
                    Integer.parseInt(columns[2].trim()),
                    parseLongLenient(columns[3].trim()),
                    new GeoPoint(rawLatitude / COORDINATE_SCALE, rawLongitude / COORDINATE_SCALE),
                    rawLatitude,
                    rawLongitude,
                    Integer.parseInt(columns[6].trim()),
                    Integer.parseInt(columns[7].trim()),
                    parseLongLenient(columns[8].trim()),
                    parseLongLenient(columns[9].trim()),
                    datagramDate,
                    Integer.parseInt(columns[11].trim())
            ));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public boolean hasExpectedColumnCount(String line) {
        return splitCsvLine(line).length == EXPECTED_COLUMNS;
    }

    /**
     * Parse a long that may arrive in scientific notation (e.g. "6.255401365E9").
     * Strategy: try {@link Long#parseLong} first; on failure fall back to
     * {@link Double#parseDouble} + cast (consistent with the reference ICE project
     * which used {@code (int) Double.parseDouble} for the same column).
     */
    private static long parseLongLenient(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return (long) Double.parseDouble(value);
        }
    }

    private String[] splitCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char value = line.charAt(i);
            if (value == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (value == ',' && !inQuotes) {
                columns.add(current.toString());
                current.setLength(0);
            } else {
                current.append(value);
            }
        }

        columns.add(current.toString());
        return columns.toArray(String[]::new);
    }
}
