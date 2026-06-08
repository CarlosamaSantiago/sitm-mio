package edu.icesi.sitmmio.analyticsstore.domain;

import edu.icesi.sitmmio.domain.SpeedResult;

import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ReportIndex {
    private final Map<String, SpeedResult> byKey = new ConcurrentHashMap<>();

    public void put(SpeedResult r) {
        byKey.put(key(r.lineId(), r.yearMonth()), r);
    }

    public Optional<SpeedResult> get(int lineId, int year, int month) {
        return Optional.ofNullable(byKey.get(key(lineId, YearMonth.of(year, month))));
    }

    public List<SpeedResult> getMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<SpeedResult> out = new ArrayList<>();
        for (SpeedResult r : byKey.values()) if (r.yearMonth().equals(ym)) out.add(r);
        return out;
    }

    public List<SpeedResult> getRange(int yf, int mf, int yt, int mt) {
        YearMonth from = YearMonth.of(yf, mf), to = YearMonth.of(yt, mt);
        List<SpeedResult> out = new ArrayList<>();
        for (SpeedResult r : byKey.values()) {
            YearMonth ym = r.yearMonth();
            if (!ym.isBefore(from) && !ym.isAfter(to)) out.add(r);
        }
        return out;
    }

    public int size() { return byKey.size(); }

    private static String key(int lineId, YearMonth ym) { return lineId + "@" + ym; }
}
