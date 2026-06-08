package edu.icesi.sitmmio.publicapi;

import edu.icesi.sitmmio.publicapi.service.RateLimiter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {
    @Test
    void allowsUntilLimit() {
        RateLimiter r = new RateLimiter();
        for (int i = 0; i < 5; i++) assertTrue(r.allow("u1", 5));
        assertFalse(r.allow("u1", 5));
    }
}
