package com.failframe.collector.api.dto;

import java.time.Instant;

public record FailureEventFilter(
        Integer statusCode,
        String serviceName,
        String environment,
        Instant fromDate,
        Instant toDate) {
}
