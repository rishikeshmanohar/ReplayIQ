package com.failframe.collector.api.dto;

public record FailureIngestionResponse(
        Long id,
        String traceId) {
}
