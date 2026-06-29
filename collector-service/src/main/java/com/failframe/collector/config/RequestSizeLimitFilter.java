package com.failframe.collector.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxRequestBytes;

    public RequestSizeLimitFilter(@Value("${failframe.security.max-request-bytes}") long maxRequestBytes) {
        this.maxRequestBytes = maxRequestBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long contentLength = request.getContentLengthLong();
        if (contentLength > maxRequestBytes) {
            response.sendError(HttpStatus.PAYLOAD_TOO_LARGE.value(), "Request body is too large");
            return;
        }

        try {
            filterChain.doFilter(new LimitedBodyRequest(request, maxRequestBytes), response);
        } catch (RequestBodyTooLargeException ex) {
            if (!response.isCommitted()) {
                response.sendError(HttpStatus.PAYLOAD_TOO_LARGE.value(), "Request body is too large");
            }
        }
    }

    private static class LimitedBodyRequest extends HttpServletRequestWrapper {

        private final long maxRequestBytes;

        LimitedBodyRequest(HttpServletRequest request, long maxRequestBytes) {
            super(request);
            this.maxRequestBytes = maxRequestBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new LimitedServletInputStream(super.getInputStream(), maxRequestBytes);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    private static class LimitedServletInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long maxRequestBytes;
        private long bytesRead;

        LimitedServletInputStream(ServletInputStream delegate, long maxRequestBytes) {
            this.delegate = delegate;
            this.maxRequestBytes = maxRequestBytes;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value != -1) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = delegate.read(buffer, offset, length);
            if (count > 0) {
                count(count);
            }
            return count;
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }

        private void count(int nextBytes) throws RequestBodyTooLargeException {
            bytesRead += nextBytes;
            if (bytesRead > maxRequestBytes) {
                throw new RequestBodyTooLargeException();
            }
        }
    }

    private static class RequestBodyTooLargeException extends IOException {
    }
}
