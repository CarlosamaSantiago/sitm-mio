package edu.icesi.sitmmio.publicapi.domain;

public final class JsonMapper {
    private JsonMapper() {}

    public static String reportToJson(SITM.SpeedReport r) {
        return "{"
                + "\"lineId\":" + r.lineId + ","
                + "\"shortName\":\"" + escape(r.shortName) + "\","
                + "\"description\":\"" + escape(r.description) + "\","
                + "\"yearMonth\":\"" + r.year + "-" + String.format("%02d", r.month) + "\","
                + "\"averageSpeedKmH\":" + r.averageSpeedKmH + ","
                + "\"totalDistanceKm\":" + r.totalDistanceKm + ","
                + "\"totalTimeHours\":" + r.totalTimeHours + ","
                + "\"validSegments\":" + r.validSegments + ","
                + "\"skippedSegments\":" + r.skippedSegments + ","
                + "\"status\":\"" + r.status + "\""
                + "}";
    }

    public static String reportsToJsonArray(SITM.SpeedReport[] reports) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < reports.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(reportToJson(reports[i]));
        }
        return sb.append("]").toString();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
