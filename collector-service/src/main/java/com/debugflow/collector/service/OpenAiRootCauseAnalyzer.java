package com.debugflow.collector.service;

import com.debugflow.collector.config.DebugFlowAiProperties;
import com.debugflow.collector.domain.ApiFailureEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class OpenAiRootCauseAnalyzer implements RootCauseAnalyzer {

    private static final int MAX_STACK_TRACE_CHARS = 4_000;
    private static final int MAX_BODY_CHARS = 2_000;
    private static final int MAX_HEADERS_CHARS = 1_500;
    private static final List<RedactionRule> SENSITIVE_TEXT_PATTERNS = List.of(
            new RedactionRule(Pattern.compile("(?i)(authorization\\s*[:=]\\s*)([^\\n,}]+)"), "$1[redacted]"),
            new RedactionRule(Pattern.compile("(?i)(cookie\\s*[:=]\\s*)([^\\n,}]+)"), "$1[redacted]"),
            new RedactionRule(Pattern.compile("(?i)(set-cookie\\s*[:=]\\s*)([^\\n,}]+)"), "$1[redacted]"),
            new RedactionRule(Pattern.compile("(?i)(x-api-key\\s*[:=]\\s*)([^\\n,}]+)"), "$1[redacted]"),
            new RedactionRule(Pattern.compile("(?i)(x-debugflow-api-key\\s*[:=]\\s*)([^\\n,}]+)"), "$1[redacted]"),
            new RedactionRule(Pattern.compile("(?i)((api[_-]?key|access[_-]?token|refresh[_-]?token)\"?\\s*[:=]\\s*\")([^\"]+)(\")"), "$1[redacted]$4"),
            new RedactionRule(Pattern.compile("(?i)(bearer\\s+)([a-z0-9._~+\\-/]+=*)"), "$1[redacted]"));

    private final DebugFlowAiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final RootCauseAnalyzer fallbackAnalyzer;

    public OpenAiRootCauseAnalyzer(
            DebugFlowAiProperties properties,
            ObjectMapper objectMapper,
            RestClient restClient,
            RootCauseAnalyzer fallbackAnalyzer) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
        this.fallbackAnalyzer = fallbackAnalyzer;
    }

    @Override
    public RootCauseAnalysisResult analyze(ApiFailureEvent event) {
        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(chatCompletionRequest(event))
                    .retrieve()
                    .body(JsonNode.class);

            String content = response == null
                    ? null
                    : response.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                return fallbackAnalyzer.analyze(event);
            }
            return parseAnalysis(content, event);
        } catch (RuntimeException ex) {
            return fallbackAnalyzer.analyze(event);
        }
    }

    private ChatCompletionRequest chatCompletionRequest(ApiFailureEvent event) {
        return new ChatCompletionRequest(
                properties.model(),
                List.of(
                        new Message("system", systemPrompt()),
                        new Message("user", buildUserPrompt(event))),
                0.1,
                new ResponseFormat("json_object"));
    }

    private String systemPrompt() {
        return """
                You are DebugFlow's root-cause analysis engine for backend API failures.
                Return only valid JSON with these fields:
                summary: string
                likelyCause: string
                suggestedFix: string
                confidence: number between 0 and 1
                Be concise, practical, and avoid inventing facts not supported by the failure data.
                """;
    }

    private String buildUserPrompt(ApiFailureEvent event) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("serviceName", event.getServiceName());
        payload.put("environment", event.getEnvironment());
        payload.put("traceId", event.getTraceId());
        payload.put("spanId", event.getSpanId());
        payload.put("httpMethod", event.getHttpMethod());
        payload.put("path", event.getPath());
        payload.put("queryString", event.getQueryString());
        payload.put("statusCode", event.getStatusCode());
        if (event.getLatencyMs() != null) {
            payload.put("latencyMs", event.getLatencyMs());
        }
        payload.put("exceptionType", event.getExceptionType());
        payload.put("exceptionMessage", safeText(event.getExceptionMessage(), MAX_BODY_CHARS));
        payload.put("stackTrace", safeText(event.getStackTrace(), MAX_STACK_TRACE_CHARS));
        payload.put("requestHeaders", truncateJson(sanitizeHeaders(event.getRequestHeadersJson()), MAX_HEADERS_CHARS));
        payload.put("responseHeaders", truncateJson(sanitizeHeaders(event.getResponseHeadersJson()), MAX_HEADERS_CHARS));
        payload.put("requestBody", safeText(event.getRequestBody(), MAX_BODY_CHARS));
        payload.put("responseBody", safeText(event.getResponseBody(), MAX_BODY_CHARS));

        return "Analyze this API failure and produce the requested JSON response:\n" + payload;
    }

    private RootCauseAnalysisResult parseAnalysis(String content, ApiFailureEvent event) {
        try {
            JsonNode parsed = objectMapper.readTree(content);
            String summary = requiredText(parsed, "summary");
            String likelyCause = requiredText(parsed, "likelyCause");
            String suggestedFix = requiredText(parsed, "suggestedFix");
            double confidence = clamp(parsed.path("confidence").asDouble(0.5));
            return new RootCauseAnalysisResult(summary, likelyCause, suggestedFix, confidence);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            return fallbackAnalyzer.analyze(event);
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + field);
        }
        return value;
    }

    private JsonNode sanitizeHeaders(JsonNode headers) {
        if (headers == null || !headers.isObject()) {
            return null;
        }

        ObjectNode sanitized = objectMapper.createObjectNode();
        headers.fields().forEachRemaining(entry -> {
            if (!isSensitiveHeader(entry.getKey())) {
                sanitized.set(entry.getKey(), entry.getValue());
            }
        });
        return sanitized;
    }

    private boolean isSensitiveHeader(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.equals("authorization")
                || normalized.equals("cookie")
                || normalized.equals("set-cookie")
                || normalized.equals("x-api-key")
                || normalized.equals("x-debugflow-api-key")
                || normalized.contains("api-key")
                || normalized.contains("apikey")
                || normalized.contains("api_key");
    }

    private String truncateJson(JsonNode node, int maxChars) {
        if (node == null) {
            return null;
        }
        return safeText(node.toString(), maxChars);
    }

    private String safeText(String value, int maxChars) {
        if (value == null) {
            return null;
        }
        return truncate(redactSensitiveText(value), maxChars);
    }

    private String redactSensitiveText(String value) {
        String redacted = value;
        for (RedactionRule rule : SENSITIVE_TEXT_PATTERNS) {
            redacted = rule.pattern().matcher(redacted).replaceAll(rule.replacement());
        }
        return redacted;
    }

    private String truncate(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "\n[truncated]";
    }

    private double clamp(double value) {
        if (value < 0) {
            return 0;
        }
        if (value > 1) {
            return 1;
        }
        return value;
    }

    private record ChatCompletionRequest(
            String model,
            List<Message> messages,
            double temperature,
            ResponseFormat response_format) {
    }

    private record Message(String role, String content) {
    }

    private record ResponseFormat(String type) {
    }

    private record RedactionRule(Pattern pattern, String replacement) {
    }
}
