package edu.icesi.sitmmio.concurrent;

public record ConcurrentMetricsSnapshot(
        String version,
        String dataset,
        long processedRows,
        long validRows,
        long skippedRows,
        long executionTimeMs,
        double throughputRowsPerSecond,
        double memoryUsedMb,
        long resultRows,
        int workerCount
) {
    public String summary() {
        return "Summary: processedRows=" + processedRows
                + ", validRows=" + validRows
                + ", skippedRows=" + skippedRows
                + ", executionTimeMs=" + executionTimeMs
                + ", throughputRowsPerSecond=" + String.format("%.2f", throughputRowsPerSecond)
                + ", memoryUsedMb=" + String.format("%.2f", memoryUsedMb)
                + ", resultRows=" + resultRows
                + ", workerCount=" + workerCount;
    }
}
