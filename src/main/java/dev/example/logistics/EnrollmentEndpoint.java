package dev.example.logistics;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnrollmentEndpoint {
    private final WorkspaceEnrollment enrollment;

    public EnrollmentEndpoint(WorkspaceEnrollment enrollment) { this.enrollment = enrollment; }

    @PostMapping("/enroll")
    public WorkspaceEnrollment.Result enroll(@RequestBody WorkspaceEnrollment.Request request) {
        return enrollment.enroll(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(InfraiGateway.ApiRejected.class)
    public ResponseEntity<Map<String, String>> rejected(InfraiGateway.ApiRejected e) {
        HttpStatus status = e.status() >= 400 && e.status() < 500
                ? HttpStatus.valueOf(e.status()) : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("error", e.code(), "message", e.getMessage()));
    }
}
