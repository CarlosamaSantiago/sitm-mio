package edu.icesi.sitmmio.citizencli.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

public final class TokenStore {
    private static final Path FILE = Paths.get(System.getProperty("user.home"), ".sitmmio", "token");

    public static void save(String jwt) throws IOException {
        Files.createDirectories(FILE.getParent());
        Files.writeString(FILE, jwt);
        try { Files.setPosixFilePermissions(FILE, PosixFilePermissions.fromString("rw-------")); }
        catch (Exception ignored) {}
    }

    public static String load() throws IOException {
        if (!Files.exists(FILE)) return null;
        return Files.readString(FILE).trim();
    }
}
