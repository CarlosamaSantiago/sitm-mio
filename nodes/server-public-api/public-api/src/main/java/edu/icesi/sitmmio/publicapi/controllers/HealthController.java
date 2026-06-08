package edu.icesi.sitmmio.publicapi.controllers;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

public final class HealthController {
    public void handle(HttpExchange ex) throws IOException {
        byte[] data = "{\"status\":\"OK\"}".getBytes();
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }
}
