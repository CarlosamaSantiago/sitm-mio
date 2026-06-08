package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.concurrent.ConcurrentCalculationResult;
import edu.icesi.sitmmio.concurrent.ConcurrentMetricsSnapshot;
import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.Route;
import edu.icesi.sitmmio.domain.RouteMonthKey;
import edu.icesi.sitmmio.domain.SpeedResult;
import edu.icesi.sitmmio.io.DatagramCsvReader;
import edu.icesi.sitmmio.validation.DatagramValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ConcurrentSpeedCalculator {
    private static final int QUEUE_CAPACITY = 16_384;
    private static final long PROGRESS_INTERVAL_ROWS = 500_000L;

    private final int workerCount;
    private final DatagramCsvReader datagramCsvReader;
    private final DatagramValidator datagramValidator;
    private final ConcurrentPartitioner partitioner;

    public ConcurrentSpeedCalculator(int workerCount) {
        this(workerCount, new DatagramCsvReader(), new DatagramValidator(), new ConcurrentPartitioner());
    }

    ConcurrentSpeedCalculator(
            int workerCount,
            DatagramCsvReader datagramCsvReader,
            DatagramValidator datagramValidator,
            ConcurrentPartitioner partitioner
    ) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("workerCount must be positive");
        }
        this.workerCount = workerCount;
        this.datagramCsvReader = datagramCsvReader;
        this.datagramValidator = datagramValidator;
        this.partitioner = partitioner;
    }

    public ConcurrentCalculationResult calculate(Path datagramsPath, Map<Integer, Route> activeRoutes)
            throws IOException, InterruptedException {
        long startedAt = System.nanoTime();
        List<BlockingQueue<WorkItem>> queues = new ArrayList<>();
        List<Future<WorkerResult>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);

        for (int i = 0; i < workerCount; i++) {
            BlockingQueue<WorkItem> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
            queues.add(queue);
            futures.add(executor.submit(new ConcurrentWorker(queue)));
        }

        ReaderCounts readerCounts;
        Set<YearMonth> observedMonths = new TreeSet<>();
        try {
            readerCounts = streamInput(datagramsPath, activeRoutes, queues, observedMonths);
        } finally {
            for (BlockingQueue<WorkItem> queue : queues) {
                queue.put(WorkItem.poisoned());
            }
        }

        Map<RouteMonthKey, SpeedTotals> merged = new LinkedHashMap<>();
        long validRows = 0L;
        long skippedRows = readerCounts.skippedRows;
        try {
            for (Future<WorkerResult> future : futures) {
                WorkerResult result = getWorkerResult(future);
                validRows += result.validRows();
                skippedRows += result.skippedRows();
                result.accumulators().forEach((key, totals) ->
                        merged.computeIfAbsent(key, ignored -> new SpeedTotals()).merge(totals));
            }
        } finally {
            executor.shutdownNow();
        }

        List<SpeedResult> results = buildResults(activeRoutes, observedMonths, merged);
        long elapsedMs = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
        double elapsedSeconds = elapsedMs / 1000.0;
        double throughput = elapsedSeconds > 0.0
                ? readerCounts.processedRows / elapsedSeconds
                : readerCounts.processedRows;
        Runtime runtime = Runtime.getRuntime();
        double memoryUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);

        ConcurrentMetricsSnapshot metrics = new ConcurrentMetricsSnapshot(
                "concurrent",
                datagramsPath.getFileName().toString(),
                readerCounts.processedRows,
                validRows,
                skippedRows,
                elapsedMs,
                throughput,
                memoryUsedMb,
                results.size(),
                workerCount
        );
        return new ConcurrentCalculationResult(results, metrics);
    }

    private ReaderCounts streamInput(
            Path datagramsPath,
            Map<Integer, Route> activeRoutes,
            List<BlockingQueue<WorkItem>> queues,
            Set<YearMonth> observedMonths
    ) throws IOException, InterruptedException {
        long processedRows = 0L;
        long skippedRows = 0L;
        try (BufferedReader reader = Files.newBufferedReader(datagramsPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                processedRows++;
                Optional<Datagram> parsed = datagramCsvReader.parseLine(line);
                if (parsed.isEmpty() || !datagramValidator.isValid(parsed.get(), activeRoutes)) {
                    skippedRows++;
                    printProgress(processedRows);
                    continue;
                }

                Datagram datagram = parsed.get();
                observedMonths.add(YearMonth.from(datagram.datagramDate()));
                int partition = partitioner.partition(datagram, workerCount);
                queues.get(partition).put(WorkItem.datagram(datagram));
                printProgress(processedRows);
            }
        }
        return new ReaderCounts(processedRows, skippedRows);
    }

    private WorkerResult getWorkerResult(Future<WorkerResult> future) throws InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Concurrent worker failed", exception.getCause());
        }
    }

    private List<SpeedResult> buildResults(
            Map<Integer, Route> activeRoutes,
            Set<YearMonth> observedMonths,
            Map<RouteMonthKey, SpeedTotals> accumulators
    ) {
        List<Route> routes = activeRoutes.values().stream()
                .sorted(Comparator.comparingInt(Route::lineId))
                .toList();
        List<SpeedResult> results = new ArrayList<>();

        for (YearMonth month : observedMonths) {
            for (Route route : routes) {
                RouteMonthKey key = new RouteMonthKey(route.lineId(), month);
                SpeedTotals totals = accumulators.getOrDefault(key, new SpeedTotals());
                boolean hasData = totals.validSegments() > 0L;
                double averageSpeed = hasData
                        ? totals.totalDistanceKm() / totals.totalTimeHours()
                        : 0.0;
                results.add(new SpeedResult(
                        route.lineId(),
                        route.shortName(),
                        route.description(),
                        month,
                        totals.totalDistanceKm(),
                        totals.totalTimeHours(),
                        averageSpeed,
                        totals.validSegments(),
                        totals.skippedSegments(),
                        hasData ? "OK" : "NO_DATA"
                ));
            }
        }

        return results;
    }

    private void printProgress(long processedRows) {
        if (processedRows % PROGRESS_INTERVAL_ROWS == 0L) {
            System.out.println("Processed rows: " + processedRows);
        }
    }

    private record ReaderCounts(long processedRows, long skippedRows) {
    }
}
