package com.failframe.collector.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record FailureIngestionRequest(
        @NotBlank String serviceName,
        String environment,
        String traceId,
        String spanId,
        @NotBlank String httpMethod,
        @NotBlank String path,
        String queryString,
        @NotNull @Min(100) @Max(599) Integer statusCode,
        Long latencyMs,
        String exceptionType,
        String exceptionMessage,
        String stackTrace,
        JsonNode requestHeaders,
        String requestBody,
        JsonNode responseHeaders,
        String responseBody,
        Instant occurredAt) {
}
