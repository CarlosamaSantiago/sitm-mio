package edu.icesi.sitmmio.citizencli.io;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class ApiClient {

    private final String baseUrl;
    private final HttpClient http;
    private final long timeoutMs;

    public ApiClient(String baseUrl, long timeoutMs) {
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public HttpResponse<String> getWithBearer(String path, String jwt) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofMillis(timeoutMs))
                .GET();
        if (jwt != null) b.header("Authorization", "Bearer " + jwt);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
}
