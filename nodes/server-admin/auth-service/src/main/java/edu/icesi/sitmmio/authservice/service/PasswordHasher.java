package edu.icesi.sitmmio.authservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/** Hash SHA-256(salt|password). Suficiente para piloto; migrar a bcrypt en producción. */
public final class PasswordHasher {
    public static String hash(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
