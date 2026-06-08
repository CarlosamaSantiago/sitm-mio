package edu.icesi.sitmmio.concurrent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class ConcurrentMetricsWriter {
    public void write(Path path, ConcurrentMetricsSnapshot metrics) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("version,dataset,processedRows,validRows,skippedRows,executionTimeMs,throughputRowsPerSecond,memoryUsedMb,resultRows,workerCount");
            writer.newLine();
            writer.write(String.format(Locale.US,
                    "%s,%s,%d,%d,%d,%d,%.2f,%.2f,%d,%d",
                    quote(metrics.version()),
                    quote(metrics.dataset()),
                    metrics.processedRows(),
                    metrics.validRows(),
                    metrics.skippedRows(),
                    metrics.executionTimeMs(),
                    metrics.throughputRowsPerSecond(),
                    metrics.memoryUsedMb(),
                    metrics.resultRows(),
                    metrics.workerCount()));
            writer.newLine();
        }
    }

    private static String quote(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
