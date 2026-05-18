package com.debugflow.sdk;

import jakarta.servlet.http.HttpServletRequest;

public class TraceContextExtractor {

    public TraceContext extract(HttpServletRequest request) {
        String traceparent = request.getHeader("traceparent");
        TraceContext traceContext = fromTraceparent(traceparent);
        if (traceContext.traceId() != null) {
            return traceContext;
        }

        String traceId = firstPresent(
                request.getHeader("X-B3-TraceId"),
                request.getHeader("X-Trace-Id"),
                request.getHeader("X-Request-Id"));
        String spanId = firstPresent(request.getHeader("X-B3-SpanId"), request.getHeader("X-Span-Id"));
        return new TraceContext(traceId, spanId);
    }

    private TraceContext fromTraceparent(String traceparent) {
        if (traceparent == null || traceparent.isBlank()) {
            return new TraceContext(null, null);
        }

        String[] parts = traceparent.split("-");
        if (parts.length < 4) {
            return new TraceContext(null, null);
        }
        return new TraceContext(parts[1], parts[2]);
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
