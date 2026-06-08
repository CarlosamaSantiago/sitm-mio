package edu.icesi.sitmmio.domain;

import java.time.YearMonth;

public record RouteMonthKey(int lineId, YearMonth yearMonth) implements Comparable<RouteMonthKey> {
    @Override
    public int compareTo(RouteMonthKey other) {
        int monthComparison = yearMonth.compareTo(other.yearMonth);
        if (monthComparison != 0) {
            return monthComparison;
        }
        return Integer.compare(lineId, other.lineId);
    }
}
