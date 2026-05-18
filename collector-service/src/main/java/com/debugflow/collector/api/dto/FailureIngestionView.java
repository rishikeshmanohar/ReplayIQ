package com.debugflow.collector.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

public record FailureIngestionView(
        Long id,
        String serviceName,
        String environment,
        String traceId,
        String spanId,
        String httpMethod,
        String path,
        String queryString,
        Integer statusCode,
        Long latencyMs,
        String exceptionType,
        String exceptionMessage,
        String stackTrace,
        JsonNode requestHeaders,
        String requestBody,
        JsonNode responseHeaders,
        String responseBody,
        Instant occurredAt,
        Instant createdAt,
        RootCauseAnalysisView rootCauseAnalysis,
        List<ReplayAttemptView> replayAttempts) {
}
