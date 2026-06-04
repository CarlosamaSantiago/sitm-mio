package edu.icesi.sitmmio.publicapi;

import com.sun.net.httpserver.HttpServer;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import edu.icesi.sitmmio.publicapi.controllers.HealthController;
import edu.icesi.sitmmio.publicapi.controllers.SpeedController;
import edu.icesi.sitmmio.publicapi.service.JwtValidator;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        int httpPort = 8080;
        String authProxy = "AuthService:default -h 127.0.0.1 -p 10040";
        String reportsProxy = "ReportProvider:default -h 127.0.0.1 -p 10060";
        for (int i = 0; i < args.length - 1; i++) {
            if ("--http-port".equals(args[i])) httpPort = Integer.parseInt(args[i + 1]);
            if ("--auth-proxy".equals(args[i])) authProxy = args[i + 1];
            if ("--reports-proxy".equals(args[i])) reportsProxy = args[i + 1];
        }
        try (Communicator c = Util.initialize(args)) {
            SITM.AuthServicePrx auth = tryAuth(c, authProxy);
            SITM.ReportProviderPrx reports = tryReports(c, reportsProxy);
            if (reports == null) throw new IllegalStateException("ReportProvider required");

            JwtValidator jwt = new JwtValidator(auth);
            SpeedController speedCtrl = new SpeedController(reports, jwt);
            HealthController healthCtrl = new HealthController();

            HttpServer server = HttpServer.create(new InetSocketAddress(httpPort), 0);
            server.createContext("/api/v1/speeds", ex -> speedCtrl.handle(ex));
            server.createContext("/api/v1/health", ex -> healthCtrl.handle(ex));
            server.createContext("/api/v1/openapi.yaml", ex -> {
                try (InputStream in = Main.class.getResourceAsStream("/openapi.yaml")) {
                    byte[] data = in != null ? in.readAllBytes() : "".getBytes();
                    ex.getResponseHeaders().add("Content-Type", "application/yaml");
                    ex.sendResponseHeaders(200, data.length);
                    try (OutputStream os = ex.getResponseBody()) { os.write(data); }
                }
            });
            server.setExecutor(Executors.newFixedThreadPool(8));
            server.start();
            System.out.println("[public-api] HTTP on " + httpPort);
            c.waitForShutdown();
        } catch (Exception e) {
            System.err.println("[public-api] FATAL: " + e);
            System.exit(1);
        }
    }

    private static SITM.AuthServicePrx tryAuth(Communicator c, String p) {
        try { return SITM.AuthServicePrx.checkedCast(c.stringToProxy(p)); }
        catch (Exception e) { return null; }
    }
    private static SITM.ReportProviderPrx tryReports(Communicator c, String p) {
        try { return SITM.ReportProviderPrx.checkedCast(c.stringToProxy(p)); }
        catch (Exception e) { return null; }
    }
}
