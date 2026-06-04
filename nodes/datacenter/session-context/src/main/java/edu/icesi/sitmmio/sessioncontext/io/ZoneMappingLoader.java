package edu.icesi.sitmmio.sessioncontext.io;

import edu.icesi.sitmmio.sessioncontext.domain.ZoneMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ZoneMappingLoader {
    public static ZoneMap load(Path file) throws IOException {
        ZoneMap m = new ZoneMap();
        if (!Files.exists(file)) return m;
        String json = Files.readString(file);
        Matcher lineMatcher = Pattern.compile("\"lineToZone\"\\s*:\\s*\\{([^}]*)\\}").matcher(json);
        if (lineMatcher.find()) parseKv(lineMatcher.group(1), (k, v) -> m.putLine(Integer.parseInt(k), Integer.parseInt(v)));
        Matcher userMatcher = Pattern.compile("\"userToZone\"\\s*:\\s*\\{([^}]*)\\}").matcher(json);
        if (userMatcher.find()) parseKv(userMatcher.group(1), (k, v) -> m.putUser(k, Integer.parseInt(v)));
        return m;
    }

    private static void parseKv(String body, java.util.function.BiConsumer<String,String> sink) {
        Matcher kv = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\d+)").matcher(body);
        while (kv.find()) sink.accept(kv.group(1), kv.group(2));
    }
}
