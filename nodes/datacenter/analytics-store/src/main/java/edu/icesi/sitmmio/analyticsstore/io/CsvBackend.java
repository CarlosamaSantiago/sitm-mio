package edu.icesi.sitmmio.analyticsstore.io;

import edu.icesi.sitmmio.analyticsstore.domain.ReportIndex;
import edu.icesi.sitmmio.domain.SpeedResult;
import edu.icesi.sitmmio.io.ResultCsvWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class CsvBackend {
    private final Path file;

    public CsvBackend(Path file) { this.file = file; }

    public void saveAll(ReportIndex index) throws IOException {
        java.util.List<SpeedResult> all = new java.util.ArrayList<>(
                index.getRange(1900, 1, 2999, 12));
        all.sort(java.util.Comparator
                .comparingInt(SpeedResult::lineId)
                .thenComparing(SpeedResult::yearMonth));
        new ResultCsvWriter().writeSpeedResults(file, all);
    }

    public int loadInto(ReportIndex index) throws IOException {
        if (!Files.isRegularFile(file)) {
            return 0;
        }
        int loaded = 0;
        try (var lines = Files.lines(file, StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) lines::iterator) {
                if (line.isBlank() || line.startsWith("lineId,")) {
                    continue;
                }
                SpeedResult result = parseResult(line);
                index.put(result);
                loaded++;
            }
        }
        return loaded;
    }

    private static SpeedResult parseResult(String line) {
        List<String> columns = splitCsv(line);
        if (columns.size() != 10) {
            throw new IllegalArgumentException("Invalid speed result row: " + line);
        }
        return new SpeedResult(
                Integer.parseInt(columns.get(0)),
                columns.get(1),
                columns.get(2),
                YearMonth.parse(columns.get(3)),
                Double.parseDouble(columns.get(4)),
                Double.parseDouble(columns.get(5)),
                Double.parseDouble(columns.get(6)),
                Long.parseLong(columns.get(7)),
                Long.parseLong(columns.get(8)),
                columns.get(9)
        );
    }

    private static List<String> splitCsv(String line) {
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
        return columns;
    }
}
