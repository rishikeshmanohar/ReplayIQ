package com.failframe.collector.api.dto;

import java.time.Instant;

public record FailureEventResponse(
        Long id,
        Long projectId,
        String serviceName,
        String environment,
        String traceId,
        String spanId,
        String httpMethod,
        String method,
        String path,
        String queryString,
        Integer statusCode,
        Long latencyMs,
        String exceptionType,
        String exceptionMessage,
        String stackTrace,
        String requestHeadersJson,
        String requestHeaders,
        String requestBody,
        String responseHeadersJson,
        String responseBody,
        Instant occurredAt,
        Instant createdAt,
        Instant capturedAt,
        String analysisSummary,
        String likelyCause,
        String suggestedFix,
        String suggestedAction,
        Double confidence,
        Instant analyzedAt) {
}
