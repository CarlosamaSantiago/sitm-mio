package edu.icesi.sitmmio.datalake.io;

import edu.icesi.sitmmio.datagramqueue.service.DatagramSerializer;
import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.RouteMonthKey;
import edu.icesi.sitmmio.contracts.SliceMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class LakeReader {
    private final Path root;
    public LakeReader(Path root) { this.root = root; }

    public List<RouteMonthKey> listPartitions() throws IOException {
        List<RouteMonthKey> keys = new ArrayList<>();
        if (!Files.isDirectory(root)) return keys;
        try (Stream<Path> lineDirs = Files.list(root)) {
            for (Path lineDir : (Iterable<Path>) lineDirs::iterator) {
                if (!lineDir.getFileName().toString().startsWith("lineId=")) continue;
                int lineId = Integer.parseInt(lineDir.getFileName().toString().substring(7));
                try (Stream<Path> yearDirs = Files.list(lineDir)) {
                    for (Path yearDir : (Iterable<Path>) yearDirs::iterator) {
                        int year = Integer.parseInt(yearDir.getFileName().toString().substring(5));
                        try (Stream<Path> monthDirs = Files.list(yearDir)) {
                            for (Path m : (Iterable<Path>) monthDirs::iterator) {
                                int month = Integer.parseInt(m.getFileName().toString().substring(6));
                                keys.add(new RouteMonthKey(lineId, YearMonth.of(year, month)));
                            }
                        }
                    }
                }
            }
        }
        return keys;
    }

    public Stream<Datagram> streamPartition(RouteMonthKey k) throws IOException {
        Path dir = root.resolve("lineId=" + k.lineId())
                       .resolve("year=" + k.yearMonth().getYear())
                       .resolve(String.format("month=%02d", k.yearMonth().getMonthValue()));
        if (!Files.isDirectory(dir)) return Stream.empty();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.toString().endsWith(".csv")).forEach(files::add);
        }
        return files.stream().flatMap(f -> {
            try {
                BufferedReader br = Files.newBufferedReader(f, StandardCharsets.UTF_8);
                return br.lines()
                        .map(l -> DatagramSerializer.fromCsv(l).orElse(null))
                        .filter(d -> d != null)
                        .map(SliceMapper::toRecord)
                        .onClose(() -> { try { br.close(); } catch (IOException ignored) {} });
            } catch (IOException e) { return Stream.empty(); }
        });
    }
}
