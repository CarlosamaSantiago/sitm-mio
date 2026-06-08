package edu.icesi.sitmmio.sessioncontext.adapter;

import com.zeroc.Ice.Current;
import edu.icesi.sitmmio.sessioncontext.domain.ZoneMap;

public class SessionContextControllerI implements SITM.SessionContextController {
    private final ZoneMap map;
    public SessionContextControllerI(ZoneMap map) { this.map = map; }

    @Override public int zoneOfLine(int lineId, Current current) { return map.zoneOfLine(lineId); }
    @Override public int zoneOfUser(String userId, Current current) { return map.zoneOfUser(userId); }
}
