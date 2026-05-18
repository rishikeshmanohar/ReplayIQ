package com.debugflow.collector.service;

import com.debugflow.collector.api.dto.FailureEventRequest;
import com.debugflow.collector.api.dto.FailureEventResponse;
import com.debugflow.collector.api.dto.RootCauseAnalysisResponse;
import com.debugflow.collector.domain.ApiFailureEvent;
import com.debugflow.collector.domain.RootCauseAnalysis;
import com.debugflow.collector.repository.ApiFailureEventRepository;
import com.debugflow.collector.repository.RootCauseAnalysisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FailureEventService {

    private static final long DEFAULT_PROJECT_ID = 1L;

    private final ApiFailureEventRepository apiFailureEventRepository;
    private final RootCauseAnalysisRepository rootCauseAnalysisRepository;
    private final RootCauseAnalyzer rootCauseAnalyzer;
    private final ObjectMapper objectMapper;

    public FailureEventService(
            ApiFailureEventRepository apiFailureEventRepository,
            RootCauseAnalysisRepository rootCauseAnalysisRepository,
            RootCauseAnalyzer rootCauseAnalyzer,
            ObjectMapper objectMapper) {
        this.apiFailureEventRepository = apiFailureEventRepository;
        this.rootCauseAnalysisRepository = rootCauseAnalysisRepository;
        this.rootCauseAnalyzer = rootCauseAnalyzer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FailureEventResponse capture(FailureEventRequest request) {
        ApiFailureEvent event = new ApiFailureEvent();
        event.setProjectId(request.projectId() == null ? DEFAULT_PROJECT_ID : request.projectId());
        event.setServiceName(request.serviceName());
        event.setEnvironment(request.environment());
        event.setTraceId(request.traceId());
        event.setSpanId(request.spanId());
        event.setHttpMethod(resolveHttpMethod(request));
        event.setPath(request.path());
        event.setQueryString(request.queryString());
        event.setStatusCode(request.statusCode());
        event.setLatencyMs(request.latencyMs());
        event.setExceptionType(request.exceptionType());
        event.setExceptionMessage(request.exceptionMessage());
        event.setStackTrace(request.stackTrace());
        event.setRequestHeadersJson(parseJson(resolveRequestHeadersJson(request), "requestHeadersJson"));
        event.setRequestBody(request.requestBody());
        event.setResponseHeadersJson(parseJson(request.responseHeadersJson(), "responseHeadersJson"));
        event.setResponseBody(request.responseBody());
        event.setOccurredAt(request.occurredAt());

        return toResponse(apiFailureEventRepository.save(event), Optional.empty());
    }

    @Transactional(readOnly = true)
    public List<FailureEventResponse> listRecent() {
        return apiFailureEventRepository.findTop50ByOrderByOccurredAtDesc().stream()
                .map(event -> toResponse(event, latestAnalysis(event.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public FailureEventResponse getById(Long id) {
        ApiFailureEvent event = getFailureEvent(id);
        return toResponse(event, latestAnalysis(event.getId()));
    }

    @Transactional
    public RootCauseAnalysisResponse analyze(Long id) {
        ApiFailureEvent event = getFailureEvent(id);
        RootCauseAnalysisResult result = rootCauseAnalyzer.analyze(event);

        RootCauseAnalysis analysis = new RootCauseAnalysis();
        analysis.setFailureEventId(event.getId());
        analysis.setSummary(result.summary());
        analysis.setLikelyCause(result.likelyCause());
        analysis.setSuggestedFix(result.suggestedFix());
        analysis.setConfidence(result.confidence());

        return toAnalysisResponse(rootCauseAnalysisRepository.save(analysis));
    }

    private ApiFailureEvent getFailureEvent(Long id) {
        return apiFailureEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Failure event not found"));
    }

    private Optional<RootCauseAnalysis> latestAnalysis(Long failureEventId) {
        return rootCauseAnalysisRepository.findTopByFailureEventIdOrderByCreatedAtDesc(failureEventId);
    }

    private FailureEventResponse toResponse(ApiFailureEvent event, Optional<RootCauseAnalysis> maybeAnalysis) {
        RootCauseAnalysis analysis = maybeAnalysis.orElse(null);
        return new FailureEventResponse(
                event.getId(),
                event.getProjectId(),
                event.getServiceName(),
                event.getEnvironment(),
                event.getTraceId(),
                event.getSpanId(),
                event.getHttpMethod(),
                event.getHttpMethod(),
                event.getPath(),
                event.getQueryString(),
                event.getStatusCode(),
                event.getLatencyMs(),
                event.getExceptionType(),
                event.getExceptionMessage(),
                event.getStackTrace(),
                jsonToString(event.getRequestHeadersJson()),
                jsonToString(event.getRequestHeadersJson()),
                event.getRequestBody(),
                jsonToString(event.getResponseHeadersJson()),
                event.getResponseBody(),
                event.getOccurredAt(),
                event.getCreatedAt(),
                event.getOccurredAt(),
                analysis == null ? null : analysis.getSummary(),
                analysis == null ? null : analysis.getLikelyCause(),
                analysis == null ? null : analysis.getSuggestedFix(),
                analysis == null ? null : analysis.getSuggestedFix(),
                analysis == null ? null : analysis.getConfidence(),
                analysis == null ? null : analysis.getCreatedAt());
    }

    private RootCauseAnalysisResponse toAnalysisResponse(RootCauseAnalysis analysis) {
        return new RootCauseAnalysisResponse(
                analysis.getId(),
                analysis.getFailureEventId(),
                analysis.getSummary(),
                analysis.getLikelyCause(),
                analysis.getSuggestedFix(),
                analysis.getSuggestedFix(),
                analysis.getConfidence(),
                analysis.getCreatedAt(),
                analysis.getCreatedAt());
    }

    private String resolveHttpMethod(FailureEventRequest request) {
        String method = request.httpMethod() == null || request.httpMethod().isBlank()
                ? request.method()
                : request.httpMethod();
        return method.toUpperCase();
    }

    private String resolveRequestHeadersJson(FailureEventRequest request) {
        return request.requestHeadersJson() == null ? request.requestHeaders() : request.requestHeadersJson();
    }

    private JsonNode parseJson(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be valid JSON", ex);
        }
    }

    private String jsonToString(JsonNode json) {
        return json == null ? null : json.toString();
    }
}
