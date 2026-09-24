package dev.example.logistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceEnrollmentTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void onlyVerifiedCompanyEmployeesJoin() throws Exception {
        class FakeDirectory implements WorkspaceEnrollment.Directory {
            boolean verified;
            int created;
            public JsonNode domain(String domain) { return json.createObjectNode(); }
            public JsonNode verify(String domain) {
                return json.createObjectNode().put("verified", verified);
            }
            public JsonNode createUser(String email, String name, String key) {
                created++;
                return json.createObjectNode().put("email", email);
            }
        }
        FakeDirectory directory = new FakeDirectory();
        WorkspaceEnrollment service = new WorkspaceEnrollment(directory);
        WorkspaceEnrollment.Request request = new WorkspaceEnrollment.Request(
                "northfreight.example", "lia@northfreight.example", "Lia",
                new WorkspaceEnrollment.Shipment("SHIP-42", java.util.List.of(
                        new WorkspaceEnrollment.Event("2026-09-20T10:00:00Z", "delivered")),
                        "pod/SHIP-42.pdf", null));
        assertThrows(IllegalArgumentException.class, () -> service.enroll(request));
        assertEquals(0, directory.created);
        directory.verified = true;
        WorkspaceEnrollment.Result result = service.enroll(request);
        assertEquals("northfreight.example", result.workspaceDomain());
        assertEquals("pod/SHIP-42.pdf", result.shipment().proofOfDeliveryFile());
        assertEquals(1, directory.created);
    }
}
