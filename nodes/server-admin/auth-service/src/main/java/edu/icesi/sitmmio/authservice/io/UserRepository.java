package edu.icesi.sitmmio.authservice.io;

import edu.icesi.sitmmio.authservice.service.PasswordHasher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Lector JSON minimalista para piloto. Soporta formato:
 * [{"userId":"x","passwordHash":"...","role":"ADMIN","zoneId":0}, ...]
 * Las contraseñas en seed se aceptan en texto plano (hashedAtLoad=true).
 */
public final class UserRepository {

    public record User(String userId, String passwordHash, String role, int zoneId) {}

    private final Map<String, User> users = new HashMap<>();
    private final String salt;

    public UserRepository(String salt) { this.salt = salt; }

    public void loadFromJson(Path file) throws IOException {
        if (!Files.exists(file)) return;
        String json = Files.readString(file);
        int i = 0;
        while ((i = json.indexOf("{", i)) != -1) {
            int end = json.indexOf("}", i);
            if (end == -1) break;
            String obj = json.substring(i, end + 1);
            String userId = field(obj, "userId");
            String passwordHash = field(obj, "passwordHash");
            String role = field(obj, "role");
            int zoneId = Integer.parseInt(field(obj, "zoneId"));
            users.put(userId, new User(userId, passwordHash, role, zoneId));
            i = end + 1;
        }
    }

    public boolean authenticate(String userId, String password) {
        User u = users.get(userId);
        if (u == null) return false;
        return u.passwordHash().equals(PasswordHasher.hash(salt, password));
    }

    public User get(String userId) { return users.get(userId); }

    private static String field(String obj, String key) {
        int idx = obj.indexOf("\"" + key + "\":");
        if (idx == -1) return "";
        int start = idx + key.length() + 3;
        if (obj.charAt(start) == '"') {
            int end = obj.indexOf('"', start + 1);
            return obj.substring(start + 1, end);
        }
        int end = obj.indexOf(',', start);
        if (end == -1) end = obj.indexOf('}', start);
        return obj.substring(start, end).trim();
    }
}
