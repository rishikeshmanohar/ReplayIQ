package com.debugflow.collector.service;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class TraceContextService {

    private static final HexFormat HEX = HexFormat.of();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public TraceContext resolve(String requestTraceId, String requestSpanId, String traceparent) {
        if (hasText(requestTraceId)) {
            return new TraceContext(requestTraceId, hasText(requestSpanId) ? requestSpanId : generateSpanId());
        }

        TraceContext traceparentContext = fromTraceparent(traceparent);
        if (traceparentContext.traceId() != null) {
            return traceparentContext;
        }

        return new TraceContext(generateTraceId(), generateSpanId());
    }

    private TraceContext fromTraceparent(String traceparent) {
        if (!hasText(traceparent)) {
            return new TraceContext(null, null);
        }

        String[] parts = traceparent.split("-");
        if (parts.length < 4 || !isValidTraceId(parts[1]) || !isValidSpanId(parts[2])) {
            return new TraceContext(null, null);
        }
        return new TraceContext(parts[1], parts[2]);
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record TraceContext(String traceId, String spanId) {
    }
}
