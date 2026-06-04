package edu.icesi.sitmmio.publicapi.controllers;

import com.sun.net.httpserver.HttpExchange;
import edu.icesi.sitmmio.publicapi.domain.ConsumerProfile;
import edu.icesi.sitmmio.publicapi.domain.JsonMapper;
import edu.icesi.sitmmio.publicapi.service.JwtValidator;
import edu.icesi.sitmmio.publicapi.service.RateLimiter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

public final class SpeedController {

    private final SITM.ReportProviderPrx reports;
    private final JwtValidator jwt;
    private final RateLimiter rate = new RateLimiter();

    public SpeedController(SITM.ReportProviderPrx reports, JwtValidator jwt) {
        this.reports = reports; this.jwt = jwt;
    }

    public void handle(HttpExchange ex) throws IOException {
        URI uri = ex.getRequestURI();
        Map<String, String> q = queryMap(uri.getRawQuery());

        SITM.UserContext ctx = jwt.validate(ex.getRequestHeaders().getFirst("Authorization"));
        if (ctx == null) { send(ex, 401, "{\"error\":\"unauthorized\"}"); return; }

        ConsumerProfile profile = ConsumerProfile.from(ctx.role);
        if (!rate.allow(ctx.userId, profile.rpm)) {
            send(ex, 429, "{\"error\":\"rate_limited\"}"); return;
        }

        String path = uri.getPath();
        try {
            if (path.matches("/api/v1/speeds/\\d+")) {
                int lineId = Integer.parseInt(path.substring("/api/v1/speeds/".length()));
                int year = Integer.parseInt(q.get("year"));
                int month = Integer.parseInt(q.get("month"));
                SITM.SpeedReport r = reports.getAverageSpeed(lineId, year, month);
                send(ex, 200, JsonMapper.reportToJson(r));
            } else if (path.equals("/api/v1/speeds")) {
                int year = Integer.parseInt(q.get("year"));
                int month = Integer.parseInt(q.get("month"));
                SITM.SpeedReport[] all = reports.getMonthlyReports(year, month);
                send(ex, 200, JsonMapper.reportsToJsonArray(all));
            } else if (path.equals("/api/v1/speeds/range")) {
                int yf = Integer.parseInt(q.get("yf"));
                int mf = Integer.parseInt(q.get("mf"));
                int yt = Integer.parseInt(q.get("yt"));
                int mt = Integer.parseInt(q.get("mt"));
                send(ex, 200, JsonMapper.reportsToJsonArray(reports.getRangeReports(yf, mf, yt, mt)));
            } else {
                send(ex, 404, "{\"error\":\"not_found\"}");
            }
        } catch (SITM.NoDataForPartition e) {
            send(ex, 404, "{\"error\":\"no_data\"}");
        } catch (Exception e) {
            send(ex, 500, "{\"error\":\"internal\"}");
        }
    }

    private static Map<String, String> queryMap(String raw) {
        Map<String, String> m = new java.util.HashMap<>();
        if (raw == null) return m;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) m.put(pair.substring(0, eq), pair.substring(eq + 1));
        }
        return m;
    }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] data = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }
}
