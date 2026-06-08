package edu.icesi.sitmmio.sessioncontext.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ZoneMap {
    private final Map<Integer, Integer> lineToZone = new ConcurrentHashMap<>();
    private final Map<String, Integer> userToZone = new ConcurrentHashMap<>();

    public void putLine(int lineId, int zoneId) { lineToZone.put(lineId, zoneId); }
    public void putUser(String userId, int zoneId) { userToZone.put(userId, zoneId); }
    public int zoneOfLine(int lineId) { return lineToZone.getOrDefault(lineId, 0); }
    public int zoneOfUser(String userId) { return userToZone.getOrDefault(userId, 0); }
}
