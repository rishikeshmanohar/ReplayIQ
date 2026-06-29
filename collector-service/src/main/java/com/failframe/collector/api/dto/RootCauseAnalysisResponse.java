package com.failframe.collector.api.dto;

import java.time.Instant;

public record RootCauseAnalysisResponse(
        Long id,
        Long failureId,
        String summary,
        String likelyCause,
        String suggestedFix,
        String suggestedAction,
        Double confidence,
        Instant createdAt,
        Instant analyzedAt) {
}
