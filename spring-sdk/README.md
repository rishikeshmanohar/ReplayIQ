# DebugFlow Spring Boot SDK

This module is a Spring Boot 3 starter-style SDK. Add it to a servlet-based Spring Boot app and it automatically captures failed HTTP requests and sends them to the DebugFlow collector.

Captured events include method, path, query string, status code, latency, exception details, trace/span IDs, masked headers, and request/response bodies up to 10KB each.

## Configuration

```yaml
debugflow:
  enabled: true
  api-key: debugflow-local-dev-key
  collector-url: http://localhost:8080
  service-name: checkout-service
  environment: local
```

## Maven

```xml
<dependency>
  <groupId>com.debugflow</groupId>
  <artifactId>debugflow-spring-sdk</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Behavior

- Registers a servlet `OncePerRequestFilter` automatically.
- Sends only when `statusCode >= 500` or an exception is thrown.
- Sends asynchronously with `WebClient`.
- Fails silently if the collector is unavailable.
- Masks sensitive headers before sending: `Authorization`, `Cookie`, `Set-Cookie`, `X-Api-Key`, `X-DebugFlow-Api-Key`.
- Extracts trace context from `traceparent`, `X-B3-TraceId`, `X-B3-SpanId`, `X-Trace-Id`, `X-Span-Id`, or `X-Request-Id`.

Request bodies are captured when the downstream application reads them. This follows Spring's `ContentCachingRequestWrapper` behavior.
