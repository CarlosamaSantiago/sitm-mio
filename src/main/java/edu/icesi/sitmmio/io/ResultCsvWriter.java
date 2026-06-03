package edu.icesi.sitmmio.io;

import edu.icesi.sitmmio.domain.SpeedResult;
import edu.icesi.sitmmio.service.MetricsCollector;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class ResultCsvWriter {
    public void writeSpeedResults(Path path, List<SpeedResult> results) throws IOException {
        createParentDirectory(path);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("lineId,shortName,description,yearMonth,totalDistanceKm,totalTimeHours,averageSpeedKmH,validSegments,skippedSegments,status");
            writer.newLine();
            for (SpeedResult result : results) {
                writer.write(String.format(Locale.US,
                        "%d,%s,%s,%s,%.6f,%.9f,%.6f,%d,%d,%s",
                        result.lineId(),
                        quote(result.shortName()),
                        quote(result.description()),
                        result.yearMonth(),
                        result.totalDistanceKm(),
                        result.totalTimeHours(),
                        result.averageSpeedKmH(),
                        result.validSegments(),
                        result.skippedSegments(),
                        result.status()));
                writer.newLine();
            }
        }
    }

    public void writeMetrics(Path path, MetricsCollector.MetricsSnapshot metrics) throws IOException {
        createParentDirectory(path);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("version,dataset,processedRows,validRows,skippedRows,executionTimeMs,throughputRowsPerSecond,memoryUsedMb,resultRows");
            writer.newLine();
            writer.write(String.format(Locale.US,
                    "%s,%s,%d,%d,%d,%d,%.2f,%.2f,%d",
                    quote(metrics.version()),
                    quote(metrics.dataset()),
                    metrics.processedRows(),
                    metrics.validRows(),
                    metrics.skippedRows(),
                    metrics.executionTimeMs(),
                    metrics.throughputRowsPerSecond(),
                    metrics.memoryUsedMb(),
                    metrics.resultRows()));
            writer.newLine();
        }
    }

    private void createParentDirectory(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private String quote(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
