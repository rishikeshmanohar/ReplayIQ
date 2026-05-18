package com.debugflow.collector.service;

public record RootCauseAnalysisResult(
        String summary,
        String likelyCause,
        String suggestedFix,
        double confidence) {
}
