package edu.icesi.sitmmio.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class BulkShardSplitter {
    private static final long LOG_INTERVAL = 1_000_000L;

    private BulkShardSplitter() {
    }

    public static void main(String[] args) throws Exception {
        Cli cli = Cli.parse(args);
        Files.createDirectories(cli.outputDir);
        List<BufferedWriter> writers = openWriters(cli);
        long total = 0;
        long written = 0;
        long skipped = 0;
        long startedAt = System.nanoTime();

        System.out.println("[bulk-shard-splitter] input=" + cli.inputPath);
        System.out.println("[bulk-shard-splitter] outputDir=" + cli.outputDir + " shards=" + cli.shards);

        try (BufferedReader reader = Files.newBufferedReader(cli.inputPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                int busId = busIdOf(line);
                if (busId <= 0) {
                    skipped++;
                } else {
                    int shard = Math.floorMod(busId, cli.shards);
                    writers.get(shard).write(line);
                    writers.get(shard).newLine();
                    written++;
                }

                if (total % LOG_INTERVAL == 0L) {
                    printProgress(total, written, skipped, startedAt);
                }
            }
        } finally {
            for (BufferedWriter writer : writers) {
                writer.close();
            }
        }

        printProgress(total, written, skipped, startedAt);
        System.out.println("[bulk-shard-splitter] DONE");
    }

    private static List<BufferedWriter> openWriters(Cli cli) throws IOException {
        List<BufferedWriter> writers = new ArrayList<>(cli.shards);
        for (int i = 0; i < cli.shards; i++) {
            Path shardPath = cli.outputDir.resolve(String.format("chunk-%02d.csv", i));
            writers.add(Files.newBufferedWriter(shardPath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
        }
        return writers;
    }

    private static int busIdOf(String line) {
        int column = 0;
        int start = 0;
        boolean inQuotes = false;
        for (int i = 0; i <= line.length(); i++) {
            boolean atEnd = i == line.length();
            char current = atEnd ? ',' : line.charAt(i);
            if (!atEnd && current == '"') {
                inQuotes = !inQuotes;
            } else if ((atEnd || current == ',') && !inQuotes) {
                if (column == 11) {
                    try {
                        return Integer.parseInt(line.substring(start, i).trim());
                    } catch (NumberFormatException ignored) {
                        return -1;
                    }
                }
                column++;
                start = i + 1;
            }
        }
        return -1;
    }

    private static void printProgress(long total, long written, long skipped, long startedAt) {
        double elapsedSeconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;
        double throughput = elapsedSeconds > 0.0 ? total / elapsedSeconds : total;
        System.out.printf("[bulk-shard-splitter] total=%d written=%d skipped=%d throughput=%.0f/s%n",
                total, written, skipped, throughput);
    }

    private record Cli(Path inputPath, Path outputDir, int shards) {
        private static Cli parse(String[] args) {
            Path input = null;
            Path output = Path.of("chunks");
            int shards = 20;
            for (int i = 0; i < args.length; i++) {
                if ("--input".equals(args[i]) && i + 1 < args.length) {
                    input = Path.of(args[++i]);
                } else if ("--output-dir".equals(args[i]) && i + 1 < args.length) {
                    output = Path.of(args[++i]);
                } else if ("--shards".equals(args[i]) && i + 1 < args.length) {
                    shards = Integer.parseInt(args[++i]);
                }
            }
            if (input == null) {
                throw new IllegalArgumentException("Usage: BulkShardSplitter --input <csv> --output-dir <dir> --shards <N>");
            }
            if (shards <= 0) {
                throw new IllegalArgumentException("shards must be positive");
            }
            return new Cli(input, output, shards);
        }
    }
}
