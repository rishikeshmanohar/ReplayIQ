package com.failframe.collector.service;

import com.failframe.collector.domain.ApiFailureEvent;
import java.util.Locale;

public class MockRootCauseAnalyzer implements RootCauseAnalyzer {

    @Override
    public RootCauseAnalysisResult analyze(ApiFailureEvent event) {
        String evidence = evidenceText(event);

        if (containsAny(evidence, "timeout", "timed out", "read timed out", "connection timed out")) {
            return new RootCauseAnalysisResult(
                    "The failure looks like a timeout while waiting on another component.",
                    "Downstream service or dependency timeout.",
                    "Check downstream latency, connection pool pressure, timeout settings, and retry behavior for the traced request.",
                    0.84);
        }

        if (event.getStatusCode() == 401 || event.getStatusCode() == 403) {
            return new RootCauseAnalysisResult(
                    "The request was rejected by authentication or authorization.",
                    "Authentication, authorization, or token issue.",
                    "Validate credentials, token expiry, required scopes, and authorization policy for this endpoint.",
                    0.8);
        }

        if (event.getStatusCode() == 500 && containsAny(evidence, "nullpointerexception", "null pointer")) {
            return new RootCauseAnalysisResult(
                    "The service returned a 500 with evidence of a null reference failure.",
                    "Null handling bug in application code.",
                    "Add null guards around the failing path, reproduce with the captured payload, and cover the case with a regression test.",
                    0.88);
        }

        if (containsAny(evidence, "payment", "token", "oauth")) {
            return new RootCauseAnalysisResult(
                    "The failure appears related to a payment or token validation path.",
                    "Expired or invalid OAuth/payment token.",
                    "Refresh the token, verify token expiry handling, and confirm payment gateway credentials before retrying.",
                    0.86);
        }

        if (event.getStatusCode() >= 500) {
            return new RootCauseAnalysisResult(
                    "The failure is a server-side error without a more specific mock signature.",
                    "Unhandled server-side exception or dependency failure.",
                    "Review the stack trace, response body, recent deploys, and dependency health around the failure timestamp.",
                    0.62);
        }

        if (event.getStatusCode() >= 400) {
            return new RootCauseAnalysisResult(
                    "The failure is a client-visible error response.",
                    "Request validation, authentication, authorization, or routing issue.",
                    "Compare the captured request against the API contract and confirm caller permissions.",
                    0.58);
        }

        return new RootCauseAnalysisResult(
                "The event does not match a common failure signature.",
                "Unknown failure cause.",
                "Inspect the captured request, response, and trace context before replaying.",
                0.35);
    }

    private String evidenceText(ApiFailureEvent event) {
        return String.join(
                        "\n",
                        safe(event.getExceptionType()),
                        safe(event.getExceptionMessage()),
                        safe(event.getStackTrace()),
                        safe(event.getResponseBody()))
                .toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String evidence, String... tokens) {
        for (String token : tokens) {
            if (evidence.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
