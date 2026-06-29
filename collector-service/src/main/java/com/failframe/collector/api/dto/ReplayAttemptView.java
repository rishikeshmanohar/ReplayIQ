package com.failframe.collector.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record ReplayAttemptView(
        Long id,
        Long failureEventId,
        Integer statusCode,
        JsonNode responseHeaders,
        String responseBody,
        Long latencyMs,
        Instant replayedAt) {
}
