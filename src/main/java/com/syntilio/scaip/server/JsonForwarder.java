package com.syntilio.scaip.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Forwards a JSON payload to a configurable HTTP endpoint via POST.
 * Used to send the JSON representation of a valid SCAIP message before returning ACK.
 * Waits for the response; caller should only ACK when forward returns 2xx.
 * <p>
 * Configuration (optional), system property or environment variable (env used when running via {@code mvn exec:java}):
 * <ul>
 *   <li>{@code scaip.forward.url} / {@code SCAIP_FORWARD_URL} – endpoint URL (default: https://httpbin.org/post)</li>
 *   <li>{@code scaip.forward.timeoutSeconds} / {@code SCAIP_FORWARD_TIMEOUT_SECONDS} – timeout in seconds (default: 10)</li>
 *   <li>{@code scaip.forward.simulateNon200} / {@code SCAIP_FORWARD_SIMULATE_NON200} – if "true", no request is sent and forward returns false (for testing NACK flow)</li>
 * </ul>
 * If {@code scaip.forward.url} is empty/blank, {@link #forward(String)} returns true without sending (no-op).
 * To simulate a non-2xx response via a real endpoint, use e.g. {@code https://httpbin.org/status/500}.
 */
public class JsonForwarder {

    private static final Logger log = LoggerFactory.getLogger(JsonForwarder.class);

    private static final String DEFAULT_URL = "https://httpbin.org/post";
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    private static String getConfig(String sysProp, String envVar, String defaultValue) {
        String v = System.getProperty(sysProp);
        if (v != null && !v.isBlank()) return v.trim();
        v = System.getenv(envVar);
        if (v != null && !v.isBlank()) return v.trim();
        return defaultValue != null ? defaultValue : "";
    }

    private static boolean getSimulateNon200() {
        String v = getConfig("scaip.forward.simulateNon200", "SCAIP_FORWARD_SIMULATE_NON200", "");
        return "true".equalsIgnoreCase(v);
    }

    private final String url;
    private final int timeoutSeconds;
    private final boolean simulateNon200;
    private final HttpClient client;

    public JsonForwarder() {
        this(getConfig("scaip.forward.url", "SCAIP_FORWARD_URL", DEFAULT_URL),
             parseIntOrDefault(getConfig("scaip.forward.timeoutSeconds", "SCAIP_FORWARD_TIMEOUT_SECONDS", ""), DEFAULT_TIMEOUT_SECONDS),
             getSimulateNon200());
    }

    public JsonForwarder(String url, int timeoutSeconds) {
        this(url, timeoutSeconds, false);
    }

    public JsonForwarder(String url, int timeoutSeconds, boolean simulateNon200) {
        this.url = url != null ? url.trim() : "";
        this.timeoutSeconds = Math.max(1, timeoutSeconds);
        this.simulateNon200 = simulateNon200;
        this.client = this.url.isEmpty() || this.simulateNon200
            ? null
            : HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(this.timeoutSeconds, 5)))
                .build();
    }

    /**
     * POSTs the given JSON string to the configured URL (Content-Type: application/json)
     * and waits for the response.
     *
     * @param json non-null JSON string to send
     * @return true if forwarding is disabled (empty URL), or the server responded with 2xx; false otherwise
     */
    public boolean forward(String json) {
        if (url.isEmpty()) {
            return true;
        }
        if (simulateNon200) {
            log.info("JsonForwarder: simulateNon200=true -> returning false (NACK flow)");
            return false;
        }
        if (json == null) {
            log.warn("JsonForwarder: null JSON, skipping forward");
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json; charset=UTF-8")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = response.statusCode();
            boolean ok = code >= 200 && code < 300;
            if (ok) {
                log.debug("JsonForwarder: POST to {} -> {} OK", url, code);
            } else {
                log.warn("JsonForwarder: POST to {} -> {} {}", url, code, response.body() != null ? response.body().substring(0, Math.min(200, response.body().length())) : "");
            }
            return ok;
        } catch (Exception e) {
            log.warn("JsonForwarder: POST to {} failed: {}", url, e.getMessage());
            return false;
        }
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
