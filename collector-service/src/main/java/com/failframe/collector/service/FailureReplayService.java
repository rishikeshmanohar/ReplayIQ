package com.failframe.collector.service;

import com.failframe.collector.api.dto.ReplayAttemptView;
import com.failframe.collector.api.dto.ReplayRequest;
import com.failframe.collector.domain.ApiFailureEvent;
import com.failframe.collector.domain.ReplayAttempt;
import com.failframe.collector.repository.ApiFailureEventRepository;
import com.failframe.collector.repository.ReplayAttemptRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
public class FailureReplayService {

    private static final Duration REPLAY_TIMEOUT = Duration.ofSeconds(15);
    private static final Set<String> ALLOWED_REPLAY_ENVIRONMENTS = Set.of("local", "staging");
    private static final Set<String> BLOCKED_REPLAY_HEADERS = Set.of(
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "x-failframe-api-key",
            "host",
            "content-length",
            "transfer-encoding",
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "upgrade");

    private final ApiFailureEventRepository apiFailureEventRepository;
    private final ReplayAttemptRepository replayAttemptRepository;
    private final ObjectMapper objectMapper;
    private final PayloadSizeLimiter payloadSizeLimiter;
    private final SensitiveHeaderMasker sensitiveHeaderMasker;
    private final WebClient webClient;

    public FailureReplayService(
            ApiFailureEventRepository apiFailureEventRepository,
            ReplayAttemptRepository replayAttemptRepository,
            ObjectMapper objectMapper,
            PayloadSizeLimiter payloadSizeLimiter,
            SensitiveHeaderMasker sensitiveHeaderMasker) {
        this.apiFailureEventRepository = apiFailureEventRepository;
        this.replayAttemptRepository = replayAttemptRepository;
        this.objectMapper = objectMapper;
        this.payloadSizeLimiter = payloadSizeLimiter;
        this.sensitiveHeaderMasker = sensitiveHeaderMasker;
        this.webClient = WebClient.builder().build();
    }

    public ReplayAttemptView replay(Long id, ReplayRequest request) {
        ApiFailureEvent event = apiFailureEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Failure event not found"));

        validateReplayAllowed(event, request);

        URI targetUri = buildTargetUri(request.targetBaseUrl(), event);
        HttpMethod method = parseMethod(event.getHttpMethod());
        HttpHeaders replayHeaders = replayHeaders(event.getRequestHeadersJson());

        long startedNanos = System.nanoTime();
        ReplayHttpResponse response;
        try {
            response = executeReplay(method, targetUri, replayHeaders, event.getRequestBody());
        } catch (RuntimeException ex) {
            response = failedReplayResponse(ex);
        }
        long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);

        ReplayAttempt attempt = new ReplayAttempt();
        attempt.setFailureEventId(event.getId());
        attempt.setStatusCode(response.statusCode());
        attempt.setResponseHeadersJson(sensitiveHeaderMasker.mask(headersToJson(response.headers())));
        attempt.setResponseBody(payloadSizeLimiter.limit(response.body()));
        attempt.setLatencyMs(latencyMs);
        attempt.setReplayedAt(Instant.now());

        return toView(replayAttemptRepository.save(attempt));
    }

    private void validateReplayAllowed(ApiFailureEvent event, ReplayRequest request) {
        String environment = event.getEnvironment() == null ? "" : event.getEnvironment().toLowerCase(Locale.ROOT);
        if (!ALLOWED_REPLAY_ENVIRONMENTS.contains(environment)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Replay is only allowed for local or staging events");
        }

        if (isPaymentPath(event.getPath()) && !Boolean.TRUE.equals(request.allowPaymentReplay())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payment path replay requires explicit approval");
        }
    }

    private boolean isPaymentPath(String path) {
        if (path == null) {
            return false;
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        return normalized.contains("payment") || normalized.contains("payments") || normalized.contains("charge");
    }

    private URI buildTargetUri(String targetBaseUrl, ApiFailureEvent event) {
        String trimmed = targetBaseUrl == null ? "" : targetBaseUrl.trim();
        URI baseUri;
        try {
            baseUri = URI.create(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetBaseUrl must be a valid URL");
        }

        String scheme = baseUri.getScheme();
        if (!StringUtils.hasText(scheme)
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                || !StringUtils.hasText(baseUri.getHost())
                || baseUri.getRawQuery() != null
                || baseUri.getRawFragment() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetBaseUrl must be an http(s) base URL without query or fragment");
        }

        String normalizedBase = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        String path = event.getPath().startsWith("/") ? event.getPath() : "/" + event.getPath();
        try {
            return UriComponentsBuilder.fromUriString(normalizedBase)
                    .path(path)
                    .query(event.getQueryString())
                    .build(true)
                    .toUri();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Captured path or query string cannot be replayed");
        }
    }

    private HttpMethod parseMethod(String method) {
        try {
            return HttpMethod.valueOf(method.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Captured HTTP method cannot be replayed");
        }
    }

    private HttpHeaders replayHeaders(JsonNode headersJson) {
        HttpHeaders headers = new HttpHeaders();
        if (headersJson == null || !headersJson.isObject()) {
            return headers;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = headersJson.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String headerName = field.getKey();
            if (shouldSkipHeader(headerName)) {
                continue;
            }

            JsonNode value = field.getValue();
            if (value == null || value.isNull()) {
                continue;
            }

            if (value.isArray()) {
                value.forEach(item -> addHeaderValue(headers, headerName, item));
            } else {
                addHeaderValue(headers, headerName, value);
            }
        }
        return headers;
    }

    private boolean shouldSkipHeader(String headerName) {
        return headerName == null || BLOCKED_REPLAY_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }

    private void addHeaderValue(HttpHeaders headers, String headerName, JsonNode value) {
        if (value == null || value.isNull()) {
            return;
        }
        headers.add(headerName, value.isTextual() ? value.asText() : value.toString());
    }

    private ReplayHttpResponse executeReplay(HttpMethod method, URI targetUri, HttpHeaders headers, String requestBody) {
        WebClient.RequestBodySpec bodySpec = webClient.method(method)
                .uri(targetUri)
                .headers(nextHeaders -> nextHeaders.addAll(headers));

        WebClient.RequestHeadersSpec<?> requestSpec = shouldSendBody(method, requestBody)
                ? bodySpec.bodyValue(requestBody)
                : bodySpec;

        ReplayHttpResponse response = requestSpec.exchangeToMono(this::toReplayResponse)
                .timeout(REPLAY_TIMEOUT)
                .block(REPLAY_TIMEOUT.plusSeconds(1));

        if (response == null) {
            throw new IllegalStateException("Replay did not return a response");
        }
        return response;
    }

    private boolean shouldSendBody(HttpMethod method, String requestBody) {
        return requestBody != null
                && (HttpMethod.POST.equals(method)
                || HttpMethod.PUT.equals(method)
                || HttpMethod.PATCH.equals(method)
                || HttpMethod.DELETE.equals(method));
    }

    private Mono<ReplayHttpResponse> toReplayResponse(ClientResponse response) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(response.headers().asHttpHeaders());
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new ReplayHttpResponse(response.statusCode().value(), responseHeaders, body));
    }

    private ReplayHttpResponse failedReplayResponse(RuntimeException ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return new ReplayHttpResponse(0, HttpHeaders.EMPTY, message);
    }

    private JsonNode headersToJson(HttpHeaders headers) {
        ObjectNode root = objectMapper.createObjectNode();
        headers.forEach((name, values) -> {
            ArrayNode array = root.putArray(name);
            values.forEach(array::add);
        });
        return root;
    }

    public ReplayAttemptView toView(ReplayAttempt attempt) {
        return new ReplayAttemptView(
                attempt.getId(),
                attempt.getFailureEventId(),
                attempt.getStatusCode(),
                attempt.getResponseHeadersJson(),
                attempt.getResponseBody(),
                attempt.getLatencyMs(),
                attempt.getReplayedAt());
    }

    private record ReplayHttpResponse(Integer statusCode, HttpHeaders headers, String body) {
    }
}
