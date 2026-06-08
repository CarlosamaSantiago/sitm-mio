package edu.icesi.sitmmio.bussimulator;

import edu.icesi.sitmmio.bussimulator.service.CliArgs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CliArgsTest {
    @Test
    void parsesKeyValuePairs() {
        CliArgs a = CliArgs.parse(new String[]{"--host","10.0.0.1","--port","10000","--throttle-ms","500"});
        assertEquals("10.0.0.1", a.str("host","def"));
        assertEquals(10000, a.intv("port", 0));
        assertEquals(500L, a.longv("throttle-ms", 0));
        assertEquals("def", a.str("missing","def"));
    }
}
