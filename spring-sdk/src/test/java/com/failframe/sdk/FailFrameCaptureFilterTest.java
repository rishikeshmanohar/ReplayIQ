package com.failframe.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.reactive.function.client.WebClient;

class FailFrameCaptureFilterTest {

    @Test
    void captures500Response() throws Exception {
        TestHarness harness = new TestHarness();
        MockHttpServletRequest request = request("GET", "/api/fail/downstream");
        MockHttpServletResponse response = new MockHttpServletResponse();

        harness.filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setStatus(500);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.getWriter().write("{\"error\":\"timeout\"}");
            httpResponse.getWriter().flush();
        });

        assertThat(harness.client.events).hasSize(1);
        FailFrameEvent event = harness.client.events.get(0);
        assertThat(event.serviceName()).isEqualTo("orders-service");
        assertThat(event.environment()).isEqualTo("local");
        assertThat(event.httpMethod()).isEqualTo("GET");
        assertThat(event.path()).isEqualTo("/api/fail/downstream");
        assertThat(event.statusCode()).isEqualTo(500);
        assertThat(event.responseBody()).contains("timeout");
    }

    @Test
    void capturesThrownException() {
        TestHarness harness = new TestHarness();
        MockHttpServletRequest request = request("GET", "/api/fail/runtime");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> harness.filter.doFilter(request, response, throwingChain()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");

        assertThat(harness.client.events).hasSize(1);
        FailFrameEvent event = harness.client.events.get(0);
        assertThat(event.statusCode()).isEqualTo(500);
        assertThat(event.exceptionType()).isEqualTo(RuntimeException.class.getName());
        assertThat(event.exceptionMessage()).isEqualTo("boom");
        assertThat(event.stackTrace()).contains("RuntimeException: boom");
    }

    @Test
    void masksSensitiveHeaders() throws Exception {
        TestHarness harness = new TestHarness();
        MockHttpServletRequest request = request("GET", "/api/fail/headers");
        request.addHeader("Authorization", "Bearer secret");
        request.addHeader("Cookie", "session=secret");
        request.addHeader("X-Api-Key", "secret");
        request.addHeader("X-FailFrame-Api-Key", "secret");
        request.addHeader("Content-Type", "application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();

        harness.filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setStatus(500);
            httpResponse.setHeader("Set-Cookie", "session=secret");
            httpResponse.setHeader("X-FailFrame-Api-Key", "secret");
            httpResponse.setHeader("Content-Type", "application/json");
        });

        FailFrameEvent event = harness.client.events.get(0);
        assertThat(event.requestHeaders().get("Authorization")).containsExactly("[masked]");
        assertThat(event.requestHeaders().get("Cookie")).containsExactly("[masked]");
        assertThat(event.requestHeaders().get("X-Api-Key")).containsExactly("[masked]");
        assertThat(event.requestHeaders().get("X-FailFrame-Api-Key")).containsExactly("[masked]");
        assertThat(event.requestHeaders().get("Content-Type")).containsExactly("application/json");
        assertThat(event.responseHeaders().get("Set-Cookie")).containsExactly("[masked]");
        assertThat(event.responseHeaders().get("X-FailFrame-Api-Key")).containsExactly("[masked]");
        assertThat(event.responseHeaders().get("Content-Type")).containsExactly("application/json");
    }

    @Test
    void doesNotSendEventFor200Response() throws Exception {
        TestHarness harness = new TestHarness();
        MockHttpServletRequest request = request("GET", "/api/healthy");
        MockHttpServletResponse response = new MockHttpServletResponse();

        harness.filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setStatus(200);
            httpResponse.getWriter().write("ok");
            httpResponse.getWriter().flush();
        });

        assertThat(harness.client.events).isEmpty();
    }

    @Test
    void failsSilentlyWhenCollectorIsDown() {
        FailFrameProperties properties = properties("http://127.0.0.1:9");
        FailFrameClient client = new FailFrameClient(WebClient.builder()
                .baseUrl(properties.collectorUrl())
                .defaultHeader("X-FailFrame-Api-Key", properties.apiKey())
                .build(), properties);

        assertThatCode(() -> client.capture(sampleEvent())).doesNotThrowAnyException();
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setCharacterEncoding("UTF-8");
        request.addHeader("X-Trace-Id", "trace-sdk-001");
        return request;
    }

    private static FilterChain throwingChain() {
        return (request, response) -> {
            throw new RuntimeException("boom");
        };
    }

    private static FailFrameProperties properties(String collectorUrl) {
        return new FailFrameProperties(true, "failframe-local-dev-key", collectorUrl, "orders-service", "local");
    }

    private static FailFrameEvent sampleEvent() {
        return new FailFrameEvent(
                "orders-service",
                "local",
                "trace-sdk-001",
                null,
                "GET",
                "/api/fail/downstream",
                null,
                500,
                100L,
                null,
                null,
                null,
                null,
                null,
                null,
                "{\"error\":\"timeout\"}",
                java.time.Instant.now());
    }

    private static class TestHarness {

        private final RecordingFailFrameClient client;
        private final FailFrameCaptureFilter filter;

        TestHarness() {
            FailFrameProperties properties = properties("http://collector.test");
            this.client = new RecordingFailFrameClient(properties);
            this.filter = new FailFrameCaptureFilter(
                    properties,
                    client,
                    new HeaderMasker(),
                    new TraceContextExtractor());
        }
    }

    private static class RecordingFailFrameClient extends FailFrameClient {

        private final List<FailFrameEvent> events = new ArrayList<>();

        RecordingFailFrameClient(FailFrameProperties properties) {
            super(WebClient.builder().baseUrl(properties.collectorUrl()).build(), properties);
        }

        @Override
        public void capture(FailFrameEvent event) {
            events.add(event);
        }
    }
}
