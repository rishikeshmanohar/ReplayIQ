package com.failframe.collector.api.dto;

import java.time.Instant;

public record ProjectApiKeyResponse(
        Long projectId,
        String projectName,
        String apiKey,
        Instant createdAt) {
}
