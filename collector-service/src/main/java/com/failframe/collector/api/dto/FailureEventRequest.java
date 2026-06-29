package com.failframe.collector.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record FailureEventRequest(
        Long projectId,
        @NotBlank String serviceName,
        String environment,
        String traceId,
        String spanId,
        String httpMethod,
        String method,
        @NotBlank String path,
        String queryString,
        @NotNull @Min(100) @Max(599) Integer statusCode,
        Long latencyMs,
        String exceptionType,
        String exceptionMessage,
        String stackTrace,
        String requestHeadersJson,
        String requestHeaders,
        String requestBody,
        String responseHeadersJson,
        String responseBody,
        Instant occurredAt) {

    @AssertTrue(message = "httpMethod is required")
    public boolean hasHttpMethod() {
        return (httpMethod != null && !httpMethod.isBlank()) || (method != null && !method.isBlank());
    }
}
