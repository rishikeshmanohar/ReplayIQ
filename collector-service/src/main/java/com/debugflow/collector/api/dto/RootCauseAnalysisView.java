package com.debugflow.collector.api.dto;

import java.time.Instant;

public record RootCauseAnalysisView(
        Long id,
        Long failureEventId,
        String summary,
        String likelyCause,
        String suggestedFix,
        Double confidence,
        Instant createdAt) {
}
