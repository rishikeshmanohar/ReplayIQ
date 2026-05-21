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
- Preserves W3C `traceparent` trace/span IDs when present.
- Reads the current OpenTelemetry span through reflection when the OpenTelemetry API is available.
- Falls back to `X-B3-TraceId`, `X-B3-SpanId`, `X-Trace-Id`, `X-Span-Id`, or `X-Request-Id`.
- Generates a trace ID and span ID when no trace context exists.

Request bodies are captured when the downstream application reads them. This follows Spring's `ContentCachingRequestWrapper` behavior.

## OpenTelemetry

DebugFlow does not require OpenTelemetry, but it can preserve trace context from services that already use it.

With the OpenTelemetry Java agent:

```bash
java \
  -javaagent:/path/to/opentelemetry-javaagent.jar \
  -Dotel.service.name=checkout-service \
  -Dotel.traces.exporter=otlp \
  -Dotel.exporter.otlp.endpoint=http://localhost:4317 \
  -jar app.jar
```

If an incoming request includes a `traceparent` header, DebugFlow stores that trace ID and span ID on the failure event. If no header exists and `io.opentelemetry.api.trace.Span.current()` is available to the application, DebugFlow uses the current span context. If neither source is available, DebugFlow generates trace/span IDs so the event remains searchable.

The SDK has no required OpenTelemetry dependency. To read the current span directly, make sure your app exposes the OpenTelemetry API on its classpath, or rely on propagated `traceparent` headers from the Java agent.
