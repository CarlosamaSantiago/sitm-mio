package edu.icesi.sitmmio.publicapi.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {
    private static final class Bucket {
        long windowStartMs; int count;
    }
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** True si la solicitud está dentro del rate; false si excede. */
    public synchronized boolean allow(String key, int rpm) {
        Bucket b = buckets.computeIfAbsent(key, k -> new Bucket());
        long now = System.currentTimeMillis();
        if (now - b.windowStartMs > 60_000) { b.windowStartMs = now; b.count = 0; }
        if (b.count >= rpm) return false;
        b.count++;
        return true;
    }
}
