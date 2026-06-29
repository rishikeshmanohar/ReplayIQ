package com.failframe.collector.service;

public record RootCauseAnalysisResult(
        String summary,
        String likelyCause,
        String suggestedFix,
        double confidence) {
}
