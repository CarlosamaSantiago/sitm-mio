package edu.icesi.sitmmio.authservice;

import edu.icesi.sitmmio.authservice.domain.Jwt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTest {
    @Test
    void issueAndValidate() {
        Jwt jwt = new Jwt("secret-key");
        String token = jwt.issue("ctrl-001", "CONTROLLER", 1, 3_600_000L);
        Jwt.Claims c = jwt.parseAndValidate(token);
        assertEquals("ctrl-001", c.sub());
        assertEquals("CONTROLLER", c.role());
        assertEquals(1, c.zoneId());
    }

    @Test
    void rejectsTamperedSignature() {
        Jwt jwt = new Jwt("secret-key");
        String token = jwt.issue("admin", "ADMIN", 0, 60_000L);
        String tampered = token.substring(0, token.length() - 2) + "ZZ";
        assertThrows(IllegalArgumentException.class, () -> jwt.parseAndValidate(tampered));
    }

    @Test
    void rejectsExpired() {
        Jwt jwt = new Jwt("k");
        String token = jwt.issue("x", "ADMIN", 0, -1000L); // expirado
        assertThrows(IllegalStateException.class, () -> jwt.parseAndValidate(token));
    }
}
