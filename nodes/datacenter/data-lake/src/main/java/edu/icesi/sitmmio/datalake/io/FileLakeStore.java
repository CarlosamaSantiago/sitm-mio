package edu.icesi.sitmmio.datalake.io;

import edu.icesi.sitmmio.datagramqueue.service.DatagramSerializer;
import edu.icesi.sitmmio.datalake.service.Partitioner;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public final class FileLakeStore {
    private final Path root;
    private final Map<String, BufferedWriter> writers = new HashMap<>();
    private final LongAdder archived = new LongAdder();

    public FileLakeStore(Path root) throws IOException {
        this.root = root;
        Files.createDirectories(root);
    }

    public synchronized void archive(SITM.Datagram d) throws IOException {
        int[] k = Partitioner.keyOf(d);
        String key = k[0] + "/" + k[1] + "/" + k[2];
        BufferedWriter w = writers.computeIfAbsent(key, kk -> openWriter(k[0], k[1], k[2]));
        w.write(DatagramSerializer.toCsv(d));
        w.newLine();
        archived.increment();
    }

    private BufferedWriter openWriter(int lineId, int year, int month) {
        try {
            Path dir = root.resolve("lineId=" + lineId)
                           .resolve("year=" + year)
                           .resolve(String.format("month=%02d", month));
            Files.createDirectories(dir);
            Path file = dir.resolve("part-0.csv");
            return Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public synchronized void flush() {
        writers.values().forEach(w -> { try { w.flush(); } catch (IOException ignored) {} });
    }

    public synchronized void close() {
        writers.values().forEach(w -> { try { w.close(); } catch (IOException ignored) {} });
        writers.clear();
    }

    public long archivedTotal() { return archived.sum(); }
}
