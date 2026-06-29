package com.failframe.sdk;

import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.HexFormat;

public class TraceContextExtractor {

    private static final HexFormat HEX = HexFormat.of();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public TraceContext extract(HttpServletRequest request) {
        String traceparent = request.getHeader("traceparent");
        TraceContext traceContext = fromTraceparent(traceparent);
        if (traceContext.traceId() != null) {
            return traceContext;
        }

        traceContext = fromOpenTelemetryCurrentSpan();
        if (traceContext.traceId() != null) {
            return traceContext;
        }

        String traceId = firstPresent(
                request.getHeader("X-B3-TraceId"),
                request.getHeader("X-Trace-Id"),
                request.getHeader("X-Request-Id"));
        String spanId = firstPresent(request.getHeader("X-B3-SpanId"), request.getHeader("X-Span-Id"));
        if (traceId != null) {
            return new TraceContext(traceId, spanId == null ? generateSpanId() : spanId);
        }

        return new TraceContext(generateTraceId(), generateSpanId());
    }

    private TraceContext fromTraceparent(String traceparent) {
        if (traceparent == null || traceparent.isBlank()) {
            return new TraceContext(null, null);
        }

        String[] parts = traceparent.split("-");
        if (parts.length < 4 || !isValidTraceId(parts[1]) || !isValidSpanId(parts[2])) {
            return new TraceContext(null, null);
        }
        return new TraceContext(parts[1], parts[2]);
    }

    private TraceContext fromOpenTelemetryCurrentSpan() {
        try {
            Class<?> spanClass = Class.forName("io.opentelemetry.api.trace.Span");
            Class<?> spanContextClass = Class.forName("io.opentelemetry.api.trace.SpanContext");
            Object span = spanClass.getMethod("current").invoke(null);
            Object spanContext = spanClass.getMethod("getSpanContext").invoke(span);
            boolean valid = (Boolean) spanContextClass.getMethod("isValid").invoke(spanContext);
            if (!valid) {
                return new TraceContext(null, null);
            }

            String traceId = (String) spanContextClass.getMethod("getTraceId").invoke(spanContext);
            String spanId = (String) spanContextClass.getMethod("getSpanId").invoke(spanContext);
            return new TraceContext(traceId, spanId);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return new TraceContext(null, null);
        }
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean isValidTraceId(String traceId) {
        return traceId != null
                && traceId.matches("[0-9a-f]{32}")
                && !traceId.equals("00000000000000000000000000000000");
    }

    private boolean isValidSpanId(String spanId) {
        return spanId != null
                && spanId.matches("[0-9a-f]{16}")
                && !spanId.equals("0000000000000000");
    }

    private String generateTraceId() {
        return generateHex(16);
    }

    private String generateSpanId() {
        return generateHex(8);
    }

    private String generateHex(int bytesLength) {
        byte[] bytes = new byte[bytesLength];
        SECURE_RANDOM.nextBytes(bytes);
        return HEX.formatHex(bytes);
    }
}
