package com.failframe.collector.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TraceContextServiceTest {

    private final TraceContextService traceContextService = new TraceContextService();

    @Test
    void preservesEventTraceContext() {
        TraceContextService.TraceContext traceContext = traceContextService.resolve(
                "trace-local-001",
                "span-local-001",
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        assertThat(traceContext.traceId()).isEqualTo("trace-local-001");
        assertThat(traceContext.spanId()).isEqualTo("span-local-001");
    }

    @Test
    void preservesTraceparentWhenEventTraceIsMissing() {
        TraceContextService.TraceContext traceContext = traceContextService.resolve(
                null,
                null,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        assertThat(traceContext.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(traceContext.spanId()).isEqualTo("00f067aa0ba902b7");
    }

    @Test
    void generatesTraceContextWhenMissing() {
        TraceContextService.TraceContext traceContext = traceContextService.resolve(null, null, null);

        assertThat(traceContext.traceId()).matches("[0-9a-f]{32}");
        assertThat(traceContext.spanId()).matches("[0-9a-f]{16}");
    }
}
