package edu.icesi.sitmmio.sessioncontext;

import edu.icesi.sitmmio.sessioncontext.domain.ZoneMap;
import edu.icesi.sitmmio.sessioncontext.io.ZoneMappingLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoneMapTest {
    @Test
    void lookupAndFallback() {
        ZoneMap m = new ZoneMap();
        m.putLine(131, 1);
        m.putUser("ctrl-001", 1);
        assertEquals(1, m.zoneOfLine(131));
        assertEquals(0, m.zoneOfLine(99999));
        assertEquals(1, m.zoneOfUser("ctrl-001"));
        assertEquals(0, m.zoneOfUser("unknown"));
    }

    @Test
    void loadsJson(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("zones.json");
        Files.writeString(f, "{\"lineToZone\":{\"131\":1,\"140\":2},\"userToZone\":{\"ctrl-001\":1}}");
        ZoneMap m = ZoneMappingLoader.load(f);
        assertEquals(1, m.zoneOfLine(131));
        assertEquals(2, m.zoneOfLine(140));
        assertEquals(1, m.zoneOfUser("ctrl-001"));
    }
}
