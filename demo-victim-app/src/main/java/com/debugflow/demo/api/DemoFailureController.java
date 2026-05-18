package com.debugflow.demo.api;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoFailureController {

    @GetMapping("/healthy")
    public Map<String, Object> healthy() {
        return Map.of(
                "status", "ok",
                "service", "demo-victim-app",
                "time", Instant.now().toString());
    }

    @GetMapping("/fail/runtime")
    public void runtimeFailure() {
        throw new RuntimeException("Simulated runtime failure from demo-victim-app");
    }

    @GetMapping("/fail/downstream")
    public ResponseEntity<Map<String, Object>> downstreamFailure() throws InterruptedException {
        Thread.sleep(750);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "DOWNSTREAM_TIMEOUT",
                        "message", "Inventory service timed out after 750ms"));
    }

    @PostMapping("/payments/charge")
    public ResponseEntity<Map<String, Object>> chargePayment(
            @Valid @RequestBody ChargeRequest request,
            HttpServletResponse response) {
        if ("expired".equalsIgnoreCase(request.token())) {
            response.setHeader("X-DebugFlow-Demo", "expired-payment-token");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "PAYMENT_TOKEN_EXPIRED",
                            "message", "The payment token is expired and cannot be charged",
                            "customerId", request.customerId()));
        }

        return ResponseEntity.ok(Map.of(
                "status", "charged",
                "customerId", request.customerId(),
                "amountCents", request.amountCents()));
    }

    public record ChargeRequest(
            @NotBlank String token,
            @NotBlank String customerId,
            long amountCents) {
    }
}
