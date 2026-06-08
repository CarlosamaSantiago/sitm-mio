package edu.icesi.sitmmio.citizencli.commands;

import edu.icesi.sitmmio.citizencli.io.ApiClient;
import edu.icesi.sitmmio.citizencli.io.TokenStore;

import java.net.http.HttpResponse;

public final class Commands {

    private final ApiClient api;

    public Commands(ApiClient api) { this.api = api; }

    public int login(String user, String password) {
        // Como AuthService es Ice, el piloto puede invocarlo directamente.
        // Para mantener el CLI puro HTTP, asumimos un endpoint POST /api/v1/login si existe;
        // de lo contrario, el operador puede colocar el JWT manualmente en ~/.sitmmio/token.
        System.out.println("[citizen-cli] login flow requires PublicAPI /login endpoint (no implementado en piloto).");
        System.out.println("[citizen-cli] coloca tu JWT en ~/.sitmmio/token manualmente o expón AuthService vía HTTP.");
        return 2;
    }

    public int speed(int lineId, int year, int month) throws Exception {
        String jwt = TokenStore.load();
        if (jwt == null) { System.err.println("login required"); return 1; }
        HttpResponse<String> r = api.getWithBearer(
                "/api/v1/speeds/" + lineId + "?year=" + year + "&month=" + month, jwt);
        System.out.println(r.body());
        return r.statusCode() == 200 ? 0 : 1;
    }

    public int speeds(int year, int month, String format) throws Exception {
        String jwt = TokenStore.load();
        if (jwt == null) { System.err.println("login required"); return 1; }
        HttpResponse<String> r = api.getWithBearer(
                "/api/v1/speeds?year=" + year + "&month=" + month, jwt);
        if ("csv".equalsIgnoreCase(format)) {
            System.out.println("lineId,yearMonth,averageSpeedKmH,status");
            // Salida ingenua: el operador puede formatear el JSON con jq si quiere.
            System.out.println(r.body());
        } else {
            System.out.println(r.body());
        }
        return r.statusCode() == 200 ? 0 : 1;
    }

    public int range(int yf, int mf, int yt, int mt) throws Exception {
        String jwt = TokenStore.load();
        if (jwt == null) { System.err.println("login required"); return 1; }
        HttpResponse<String> r = api.getWithBearer(
                "/api/v1/speeds/range?yf=" + yf + "&mf=" + mf + "&yt=" + yt + "&mt=" + mt, jwt);
        System.out.println(r.body());
        return r.statusCode() == 200 ? 0 : 1;
    }

    public int health() throws Exception {
        HttpResponse<String> r = api.getWithBearer("/api/v1/health", null);
        System.out.println(r.body());
        return r.statusCode() == 200 ? 0 : 1;
    }

    public int openapi(String outFile) throws Exception {
        HttpResponse<String> r = api.getWithBearer("/api/v1/openapi.yaml", null);
        java.nio.file.Files.writeString(java.nio.file.Path.of(outFile), r.body());
        System.out.println("openapi spec written to " + outFile);
        return 0;
    }
}
