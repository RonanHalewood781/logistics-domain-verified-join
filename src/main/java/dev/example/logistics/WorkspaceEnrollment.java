package dev.example.logistics;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceEnrollment {
    public interface Directory {
        JsonNode domain(String domain);
        JsonNode verify(String domain);
        JsonNode createUser(String email, String name, String key);
    }

    private final Directory directory;

    @Autowired
    public WorkspaceEnrollment(InfraiGateway gateway) {
        this(new Directory() {
            public JsonNode domain(String domain) { return gateway.domain(domain); }
            public JsonNode verify(String domain) { return gateway.verify(domain); }
            public JsonNode createUser(String email, String name, String key) {
                return gateway.createUser(email, name, key);
            }
        });
    }

    public WorkspaceEnrollment(Directory directory) { this.directory = directory; }

    public Result enroll(Request request) {
        String domain = request.companyDomain().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (!email.endsWith("@" + domain) || domain.isBlank())
            throw new IllegalArgumentException("Employee email must belong to the company domain");
        directory.domain(domain);
        JsonNode proof = directory.verify(domain);
        if (!proof.path("verified").asBoolean(false))
            throw new IllegalArgumentException("Company domain must be verified before joining");
        String key = UUID.nameUUIDFromBytes((domain + ":" + email).getBytes(StandardCharsets.UTF_8)).toString();
        JsonNode user = directory.createUser(email, request.name(), key);
        return new Result(domain, email, user, request.shipment());
    }

    public record Shipment(String trackingNumber, List<Event> events, String proofOfDeliveryFile,
                           String exceptionReason) {}
    public record Event(String occurredAt, String status) {}
    public record Request(String companyDomain, String email, String name, Shipment shipment) {}
    public record Result(String workspaceDomain, String memberEmail, JsonNode user, Shipment shipment) {}
}
