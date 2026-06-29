package com.failframe.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class TraceContextExtractorTest {

    private final TraceContextExtractor extractor = new TraceContextExtractor();

    @Test
    void preservesTraceparentTraceContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        TraceContext traceContext = extractor.extract(request);

        assertThat(traceContext.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(traceContext.spanId()).isEqualTo("00f067aa0ba902b7");
    }

    @Test
    void fallsBackToRequestTraceHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "trace-local-001");
        request.addHeader("X-Span-Id", "span-local-001");

        TraceContext traceContext = extractor.extract(request);

        assertThat(traceContext.traceId()).isEqualTo("trace-local-001");
        assertThat(traceContext.spanId()).isEqualTo("span-local-001");
    }

    @Test
    void generatesTraceAndSpanWhenMissing() {
        TraceContext traceContext = extractor.extract(new MockHttpServletRequest());

        assertThat(traceContext.traceId()).matches("[0-9a-f]{32}");
        assertThat(traceContext.spanId()).matches("[0-9a-f]{16}");
    }
}
