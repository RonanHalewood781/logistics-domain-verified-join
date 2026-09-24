package dev.example.logistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InfraiGateway {
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json;
    private final String baseUrl;
    private final String apiKey;

    public InfraiGateway(ObjectMapper json, @Value("${infrai.base-url}") String baseUrl,
                         @Value("${infrai.api-key}") String apiKey) {
        this.json = json;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public JsonNode domain(String domain) {
        return call("GET", "/v1/dns/domain/get?domain=" + URLEncoder.encode(domain, StandardCharsets.UTF_8), null);
    }

    public JsonNode verify(String domain) {
        return call("POST", "/v1/dns/domain/verify", Map.of("domain", domain));
    }

    public JsonNode createUser(String email, String name, String idempotencyKey) {
        return call("POST", "/v1/auth/user/create",
                Map.of("email", email, "name", name, "idempotency_key", idempotencyKey));
    }

    private JsonNode call(String method, String path, Map<String, String> body) {
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(20));
                if (body == null) request.method(method, HttpRequest.BodyPublishers.noBody());
                else request.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
                HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = json.readTree(response.body());
                if (response.statusCode() == 429 && attempt < 3) {
                    long delay = response.headers().firstValue("Retry-After")
                            .map(InfraiGateway::retrySeconds).orElse(1L << attempt);
                    Thread.sleep(Math.max(1, delay) * 1000);
                    continue;
                }
                if (!envelope.path("ok").asBoolean(false)) {
                    JsonNode error = envelope.path("error");
                    throw new ApiRejected(response.statusCode(), error.path("code").asText("REQUEST_REJECTED"),
                            error.path("message").asText("Request rejected"));
                }
                if (response.statusCode() >= 500) throw new IllegalStateException("Upstream request failed");
                return envelope.path("data");
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read API response", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Request interrupted", e);
            }
        }
        throw new IllegalStateException("Retry limit reached");
    }

    private static long retrySeconds(String value) {
        try { return Long.parseLong(value); }
        catch (NumberFormatException ignored) { return 1; }
    }

    public static class ApiRejected extends RuntimeException {
        private final int status;
        private final String code;
        public ApiRejected(int status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }
        public int status() { return status; }
        public String code() { return code; }
    }
}
