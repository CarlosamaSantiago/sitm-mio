package edu.icesi.sitmmio.service;

public class MetricsCollector {
    private final String version;
    private final String dataset;
    private long processedRows;
    private long validRows;
    private long skippedRows;
    private long startedAtNanos;
    private long finishedAtNanos;
    private long resultRows;

    public MetricsCollector(String version, String dataset) {
        this.version = version;
        this.dataset = dataset;
    }

    public void start() {
        processedRows = 0;
        validRows = 0;
        skippedRows = 0;
        resultRows = 0;
        startedAtNanos = System.nanoTime();
        finishedAtNanos = startedAtNanos;
    }

    public void finish(long resultRows) {
        this.resultRows = resultRows;
        this.finishedAtNanos = System.nanoTime();
    }

    public void recordProcessed() {
        processedRows++;
    }

    public void recordValid() {
        validRows++;
    }

    public void recordSkipped() {
        skippedRows++;
    }

    public long processedRows() {
        return processedRows;
    }

    public MetricsSnapshot snapshot() {
        long elapsedMs = Math.max(0L, (finishedAtNanos - startedAtNanos) / 1_000_000L);
        double elapsedSeconds = elapsedMs / 1000.0;
        double throughput = elapsedSeconds > 0.0 ? processedRows / elapsedSeconds : processedRows;
        Runtime runtime = Runtime.getRuntime();
        double memoryUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
        return new MetricsSnapshot(
                version,
                dataset,
                processedRows,
                validRows,
                skippedRows,
                elapsedMs,
                throughput,
                memoryUsedMb,
                resultRows
        );
    }

    public String summary() {
        MetricsSnapshot metrics = snapshot();
        return "Summary: processedRows=" + metrics.processedRows()
                + ", validRows=" + metrics.validRows()
                + ", skippedRows=" + metrics.skippedRows()
                + ", executionTimeMs=" + metrics.executionTimeMs()
                + ", throughputRowsPerSecond=" + String.format("%.2f", metrics.throughputRowsPerSecond())
                + ", memoryUsedMb=" + String.format("%.2f", metrics.memoryUsedMb())
                + ", resultRows=" + metrics.resultRows();
    }

    public record MetricsSnapshot(
            String version,
            String dataset,
            long processedRows,
            long validRows,
            long skippedRows,
            long executionTimeMs,
            double throughputRowsPerSecond,
            double memoryUsedMb,
            long resultRows
    ) {
    }
}
