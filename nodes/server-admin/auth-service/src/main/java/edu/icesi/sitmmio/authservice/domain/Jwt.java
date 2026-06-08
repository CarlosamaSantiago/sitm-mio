package edu.icesi.sitmmio.authservice.domain;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * JWT HS256 minimalista.
 * Header: {"alg":"HS256","typ":"JWT"}  Payload: JSON con sub/role/zoneId/exp.
 * No usa parser JSON externo; los claims se serializan con String.format y se
 * parsean por extracción ingenua. Suficiente para piloto.
 */
public final class Jwt {

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String HEADER_B64 = b64url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));

    private final String secret;

    public Jwt(String secret) { this.secret = secret; }

    public String issue(String userId, String role, int zoneId, long ttlMs) {
        long exp = System.currentTimeMillis() / 1000 + ttlMs / 1000;
        String payload = String.format("{\"sub\":\"%s\",\"role\":\"%s\",\"zoneId\":%d,\"exp\":%d}",
                userId, role, zoneId, exp);
        String payloadB64 = b64url(payload.getBytes(StandardCharsets.UTF_8));
        String signingInput = HEADER_B64 + "." + payloadB64;
        String sig = b64url(hmac(signingInput, secret));
        return signingInput + "." + sig;
    }

    public Claims parseAndValidate(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("malformed");
        String signingInput = parts[0] + "." + parts[1];
        String expectedSig = b64url(hmac(signingInput, secret));
        if (!expectedSig.equals(parts[2])) throw new IllegalArgumentException("bad signature");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String sub = extract(payload, "sub");
        String role = extract(payload, "role");
        int zoneId = Integer.parseInt(extract(payload, "zoneId"));
        long exp = Long.parseLong(extract(payload, "exp"));
        if (System.currentTimeMillis() / 1000 > exp) throw new IllegalStateException("expired");
        return new Claims(sub, role, zoneId, exp);
    }

    private static byte[] hmac(String input, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static String b64url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String extract(String json, String key) {
        int idx = json.indexOf("\"" + key + "\":");
        if (idx == -1) throw new IllegalArgumentException("missing " + key);
        int start = idx + key.length() + 3;
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return json.substring(start + 1, end);
        }
        int end = json.indexOf(',', start);
        if (end == -1) end = json.indexOf('}', start);
        return json.substring(start, end).trim();
    }

    public record Claims(String sub, String role, int zoneId, long exp) {}
}
