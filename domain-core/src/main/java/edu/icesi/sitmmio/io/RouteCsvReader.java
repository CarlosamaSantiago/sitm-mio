package edu.icesi.sitmmio.io;

import edu.icesi.sitmmio.domain.Route;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RouteCsvReader {
    public Map<Integer, Route> readActiveRoutes(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return Map.of();
            }

            String[] headers = splitCsvLine(headerLine);
            Map<String, Integer> indexes = indexesByHeader(headers);
            int lineIdIndex = requiredIndex(indexes, "LINEID");
            int shortNameIndex = requiredIndex(indexes, "SHORTNAME");
            int descriptionIndex = requiredIndex(indexes, "DESCRIPTION");

            Map<Integer, Route> routes = new LinkedHashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = splitCsvLine(line);
                if (columns.length <= Math.max(lineIdIndex, Math.max(shortNameIndex, descriptionIndex))) {
                    continue;
                }

                try {
                    int lineId = Integer.parseInt(columns[lineIdIndex].trim());
                    routes.put(lineId, new Route(
                            lineId,
                            columns[shortNameIndex].trim(),
                            columns[descriptionIndex].trim()
                    ));
                } catch (NumberFormatException ignored) {
                    // Bad route rows should not prevent processing the valid catalog rows.
                }
            }

            return routes;
        }
    }

    private int requiredIndex(Map<String, Integer> indexes, String header) {
        Integer index = indexes.get(header);
        if (index == null) {
            throw new IllegalArgumentException("Missing required route CSV header: " + header);
        }
        return index;
    }

    private Map<String, Integer> indexesByHeader(String[] headers) {
        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            indexes.putIfAbsent(headers[i].trim().toUpperCase(Locale.ROOT), i);
        }
        return indexes;
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
