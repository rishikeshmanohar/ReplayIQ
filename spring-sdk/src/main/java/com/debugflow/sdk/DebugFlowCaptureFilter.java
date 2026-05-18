package com.debugflow.sdk;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class DebugFlowCaptureFilter extends OncePerRequestFilter {

    private final DebugFlowProperties properties;
    private final DebugFlowClient debugFlowClient;
    private final HeaderMasker headerMasker;
    private final TraceContextExtractor traceContextExtractor;

    public DebugFlowCaptureFilter(
            DebugFlowProperties properties,
            DebugFlowClient debugFlowClient,
            HeaderMasker headerMasker,
            TraceContextExtractor traceContextExtractor) {
        this.properties = properties;
        this.debugFlowClient = debugFlowClient;
        this.headerMasker = headerMasker;
        this.traceContextExtractor = traceContextExtractor;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled() || !properties.hasApiKey()) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, BodyCapture.MAX_BODY_BYTES);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        Throwable failure = null;
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } catch (IOException | ServletException | RuntimeException ex) {
            failure = ex;
            throw ex;
        } finally {
            long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
            try {
                maybeCapture(requestWrapper, responseWrapper, failure, latencyMs);
            } finally {
                responseWrapper.copyBodyToResponse();
            }
        }
    }

    private void maybeCapture(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            Throwable failure,
            long latencyMs) {
        int statusCode = failure != null && response.getStatus() == HttpServletResponse.SC_OK
                ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                : response.getStatus();
        if (statusCode < 500 && failure == null) {
            return;
        }

        TraceContext traceContext = traceContextExtractor.extract(request);
        DebugFlowEvent event = new DebugFlowEvent(
                properties.serviceName(),
                properties.environment(),
                traceContext.traceId(),
                traceContext.spanId(),
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                statusCode,
                latencyMs,
                failure == null ? null : failure.getClass().getName(),
                failure == null ? null : failure.getMessage(),
                failure == null ? null : stackTrace(failure),
                headerMasker.maskRequestHeaders(request),
                BodyCapture.fromBytes(request.getContentAsByteArray(), request.getCharacterEncoding()),
                headerMasker.maskResponseHeaders(response),
                BodyCapture.fromBytes(response.getContentAsByteArray(), response.getCharacterEncoding()),
                Instant.now());

        debugFlowClient.capture(event);
    }

    private String stackTrace(Throwable failure) {
        StringWriter writer = new StringWriter();
        failure.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
