package edu.icesi.sitmmio.tools;

import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.Route;
import edu.icesi.sitmmio.io.DatagramCsvReader;
import edu.icesi.sitmmio.io.RouteCsvReader;
import edu.icesi.sitmmio.validation.DatagramValidator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class BulkLakeBuilder {
    private static final long LOG_INTERVAL = 1_000_000L;

    private BulkLakeBuilder() {
    }

    public static void main(String[] args) throws Exception {
        Cli cli = Cli.parse(args);
        Map<Integer, Route> activeRoutes = new RouteCsvReader().readActiveRoutes(cli.routesPath);
        DatagramCsvReader reader = new DatagramCsvReader();
        DatagramValidator validator = new DatagramValidator();
        LakeWriter lakeWriter = new LakeWriter(cli.lakeRoot, cli.partId);

        long total = 0;
        long written = 0;
        long skipped = 0;
        long startedAt = System.nanoTime();

        System.out.println("[bulk-lake-builder] input=" + cli.inputPath);
        System.out.println("[bulk-lake-builder] routes=" + cli.routesPath + " active=" + activeRoutes.size());
        System.out.println("[bulk-lake-builder] lake=" + cli.lakeRoot + " partId=" + cli.partId);

        try (BufferedReader br = Files.newBufferedReader(cli.inputPath, StandardCharsets.UTF_8);
             LakeWriter writer = lakeWriter) {
            String line;
            while ((line = br.readLine()) != null) {
                total++;
                Optional<Datagram> parsed = reader.parseLine(line);
                if (parsed.isEmpty() || !validator.isValid(parsed.get(), activeRoutes)) {
                    skipped++;
                } else {
                    writer.write(parsed.get(), line);
                    written++;
                }

                if (total % LOG_INTERVAL == 0L) {
                    printProgress(total, written, skipped, startedAt);
                }
            }
        }

        printProgress(total, written, skipped, startedAt);
        System.out.println("[bulk-lake-builder] DONE");
    }

    private static void printProgress(long total, long written, long skipped, long startedAt) {
        double elapsedSeconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;
        double throughput = elapsedSeconds > 0.0 ? total / elapsedSeconds : total;
        System.out.printf("[bulk-lake-builder] total=%d written=%d skipped=%d throughput=%.0f/s%n",
                total, written, skipped, throughput);
    }

    private static final class LakeWriter implements AutoCloseable {
        private final Path lakeRoot;
        private final String partId;
        private final Map<String, BufferedWriter> writers = new HashMap<>();

        private LakeWriter(Path lakeRoot, String partId) {
            this.lakeRoot = lakeRoot;
            this.partId = sanitize(partId);
        }

        private void write(Datagram datagram, String rawLine) throws IOException {
            LocalDateTime date = datagram.datagramDate();
            Path dir = lakeRoot.resolve("lineId=" + datagram.lineId())
                    .resolve("year=" + date.getYear())
                    .resolve(String.format("month=%02d", date.getMonthValue()));
            String key = dir.toString();
            BufferedWriter writer = writers.computeIfAbsent(key, ignored -> open(dir));
            writer.write(rawLine);
            writer.newLine();
        }

        private BufferedWriter open(Path dir) {
            try {
                Files.createDirectories(dir);
                Path file = dir.resolve("part-" + partId + ".csv");
                return Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot open lake partition " + dir, e);
            }
        }

        @Override
        public void close() {
            for (BufferedWriter writer : writers.values()) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                    // Closing best effort; the process is already ending.
                }
            }
            writers.clear();
        }

        private static String sanitize(String value) {
            return value.replaceAll("[^A-Za-z0-9_.-]", "_");
        }
    }

    private static final class Cli {
        private final Path inputPath;
        private final Path routesPath;
        private final Path lakeRoot;
        private final String partId;

        private Cli(Path inputPath, Path routesPath, Path lakeRoot, String partId) {
            this.inputPath = inputPath;
            this.routesPath = routesPath;
            this.lakeRoot = lakeRoot;
            this.partId = partId;
        }

        private static Cli parse(String[] args) {
            Map<String, String> values = new HashMap<>();
            for (int i = 0; i < args.length - 1; i++) {
                if (args[i].startsWith("--")) {
                    values.put(args[i], args[i + 1]);
                    i++;
                }
            }

            String input = values.get("--input");
            String routes = values.getOrDefault("--routes", "data/raw/lines-241-ActiveGT.csv");
            String lake = values.getOrDefault("--lake", "lake");
            String partId = values.getOrDefault("--part-id", "0");
            if (input == null || input.isBlank()) {
                throw new IllegalArgumentException("Usage: BulkLakeBuilder --input <csv> --routes <routes.csv> --lake <lakeRoot> --part-id <id>");
            }
            return new Cli(Path.of(input), Path.of(routes), Path.of(lake), partId);
        }
    }
}
