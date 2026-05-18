package com.debugflow.sdk;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DebugFlowEvent(
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
        Map<String, List<String>> requestHeaders,
        String requestBody,
        Map<String, List<String>> responseHeaders,
        String responseBody,
        Instant occurredAt) {
}
