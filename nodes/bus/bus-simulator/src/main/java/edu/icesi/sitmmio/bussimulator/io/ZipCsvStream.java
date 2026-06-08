package edu.icesi.sitmmio.bussimulator.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reader streamed para CSV o ZIP. Si el path termina en .zip, abre el primer
 * entry que termine en .csv y emite líneas (sin cargar a memoria).
 */
public final class ZipCsvStream implements AutoCloseable {

    private final BufferedReader reader;
    private final AutoCloseable underlying;

    public ZipCsvStream(BufferedReader reader, AutoCloseable underlying) {
        this.reader = reader;
        this.underlying = underlying;
    }

    public static ZipCsvStream open(Path path) throws IOException {
        InputStream raw = new FileInputStream(path.toFile());
        if (path.getFileName().toString().toLowerCase().endsWith(".zip")) {
            ZipInputStream zin = new ZipInputStream(raw);
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().endsWith(".csv")) {
                    return new ZipCsvStream(
                            new BufferedReader(new InputStreamReader(zin, StandardCharsets.UTF_8)),
                            zin
                    );
                }
            }
            zin.close();
            throw new IOException("No .csv entry inside " + path);
        }
        return new ZipCsvStream(
                new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8)),
                raw
        );
    }

    public Stream<String> lines() {
        return reader.lines();
    }

    @Override
    public void close() throws IOException {
        try { reader.close(); } catch (IOException ignored) {}
        try { underlying.close(); } catch (Exception e) { throw new IOException(e); }
    }
}
