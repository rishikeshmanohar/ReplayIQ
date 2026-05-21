package com.debugflow.collector.service;

import com.debugflow.collector.api.dto.FailureEventFilter;
import com.debugflow.collector.api.dto.FailureIngestionRequest;
import com.debugflow.collector.api.dto.FailureIngestionResponse;
import com.debugflow.collector.api.dto.FailureIngestionView;
import com.debugflow.collector.api.dto.ReplayAttemptView;
import com.debugflow.collector.api.dto.RootCauseAnalysisView;
import com.debugflow.collector.domain.ApiFailureEvent;
import com.debugflow.collector.domain.Project;
import com.debugflow.collector.domain.ReplayAttempt;
import com.debugflow.collector.domain.RootCauseAnalysis;
import com.debugflow.collector.repository.ApiFailureEventRepository;
import com.debugflow.collector.repository.ReplayAttemptRepository;
import com.debugflow.collector.repository.RootCauseAnalysisRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FailureIngestionService {

    private static final int MAX_LIST_RESULTS = 100;

    private final ProjectApiKeyAuthenticator projectApiKeyAuthenticator;
    private final ApiFailureEventRepository apiFailureEventRepository;
    private final RootCauseAnalysisRepository rootCauseAnalysisRepository;
    private final ReplayAttemptRepository replayAttemptRepository;
    private final RootCauseAnalyzer rootCauseAnalyzer;
    private final SensitiveHeaderMasker sensitiveHeaderMasker;
    private final PayloadSizeLimiter payloadSizeLimiter;
    private final TraceContextService traceContextService;

    public FailureIngestionService(
            ProjectApiKeyAuthenticator projectApiKeyAuthenticator,
            ApiFailureEventRepository apiFailureEventRepository,
            RootCauseAnalysisRepository rootCauseAnalysisRepository,
            ReplayAttemptRepository replayAttemptRepository,
            RootCauseAnalyzer rootCauseAnalyzer,
            SensitiveHeaderMasker sensitiveHeaderMasker,
            PayloadSizeLimiter payloadSizeLimiter,
            TraceContextService traceContextService) {
        this.projectApiKeyAuthenticator = projectApiKeyAuthenticator;
        this.apiFailureEventRepository = apiFailureEventRepository;
        this.rootCauseAnalysisRepository = rootCauseAnalysisRepository;
        this.replayAttemptRepository = replayAttemptRepository;
        this.rootCauseAnalyzer = rootCauseAnalyzer;
        this.sensitiveHeaderMasker = sensitiveHeaderMasker;
        this.payloadSizeLimiter = payloadSizeLimiter;
        this.traceContextService = traceContextService;
    }

    @Transactional
    public FailureIngestionResponse ingest(String apiKey, String traceparent, FailureIngestionRequest request) {
        Project project = projectApiKeyAuthenticator.authenticate(apiKey);
        TraceContextService.TraceContext traceContext = traceContextService.resolve(request.traceId(), request.spanId(), traceparent);

        ApiFailureEvent event = new ApiFailureEvent();
        event.setProjectId(project.getId());
        event.setServiceName(request.serviceName());
        event.setEnvironment(blankToNull(request.environment()));
        event.setTraceId(traceContext.traceId());
        event.setSpanId(traceContext.spanId());
        event.setHttpMethod(request.httpMethod().toUpperCase());
        event.setPath(request.path());
        event.setQueryString(blankToNull(request.queryString()));
        event.setStatusCode(request.statusCode());
        event.setLatencyMs(request.latencyMs());
        event.setExceptionType(blankToNull(request.exceptionType()));
        event.setExceptionMessage(blankToNull(request.exceptionMessage()));
        event.setStackTrace(blankToNull(request.stackTrace()));
        event.setRequestHeadersJson(sensitiveHeaderMasker.mask(request.requestHeaders()));
        event.setRequestBody(payloadSizeLimiter.limit(request.requestBody()));
        event.setResponseHeadersJson(sensitiveHeaderMasker.mask(request.responseHeaders()));
        event.setResponseBody(payloadSizeLimiter.limit(request.responseBody()));
        event.setOccurredAt(request.occurredAt() == null ? Instant.now() : request.occurredAt());

        ApiFailureEvent saved = apiFailureEventRepository.save(event);
        return new FailureIngestionResponse(saved.getId(), saved.getTraceId());
    }

    @Transactional(readOnly = true)
    public List<FailureIngestionView> list(FailureEventFilter filter) {
        PageRequest pageRequest = PageRequest.of(
                0,
                MAX_LIST_RESULTS,
                Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "id")));

        return apiFailureEventRepository.findAll(toSpecification(filter), pageRequest)
                .stream()
                .map(event -> toView(event, null, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public FailureIngestionView get(Long id) {
        ApiFailureEvent event = apiFailureEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Failure event not found"));
        List<ReplayAttempt> replayAttempts = replayAttemptRepository.findByFailureEventIdOrderByReplayedAtDesc(event.getId());
        return toView(event, latestAnalysis(event.getId()).orElse(null), replayAttempts);
    }

    @Transactional
    public RootCauseAnalysisView analyze(Long id) {
        ApiFailureEvent event = apiFailureEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Failure event not found"));
        RootCauseAnalysisResult result = rootCauseAnalyzer.analyze(event);

        RootCauseAnalysis analysis = new RootCauseAnalysis();
        analysis.setFailureEventId(event.getId());
        analysis.setSummary(result.summary());
        analysis.setLikelyCause(result.likelyCause());
        analysis.setSuggestedFix(result.suggestedFix());
        analysis.setConfidence(result.confidence());

        return toAnalysisView(rootCauseAnalysisRepository.save(analysis));
    }

    private Specification<ApiFailureEvent> toSpecification(FailureEventFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.statusCode() != null) {
                predicates.add(criteriaBuilder.equal(root.get("statusCode"), filter.statusCode()));
            }
            if (filter.serviceName() != null && !filter.serviceName().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("serviceName"), filter.serviceName()));
            }
            if (filter.environment() != null && !filter.environment().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("environment"), filter.environment()));
            }
            if (filter.fromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<Instant>get("occurredAt"), filter.fromDate()));
            }
            if (filter.toDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<Instant>get("occurredAt"), filter.toDate()));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Optional<RootCauseAnalysis> latestAnalysis(Long failureEventId) {
        return rootCauseAnalysisRepository.findTopByFailureEventIdOrderByCreatedAtDesc(failureEventId);
    }

    private FailureIngestionView toView(ApiFailureEvent event, RootCauseAnalysis analysis, List<ReplayAttempt> replayAttempts) {
        return new FailureIngestionView(
                event.getId(),
                event.getServiceName(),
                event.getEnvironment(),
                event.getTraceId(),
                event.getSpanId(),
                event.getHttpMethod(),
                event.getPath(),
                event.getQueryString(),
                event.getStatusCode(),
                event.getLatencyMs(),
                event.getExceptionType(),
                event.getExceptionMessage(),
                event.getStackTrace(),
                event.getRequestHeadersJson(),
                event.getRequestBody(),
                event.getResponseHeadersJson(),
                event.getResponseBody(),
                event.getOccurredAt(),
                event.getCreatedAt(),
                analysis == null ? null : toAnalysisView(analysis),
                replayAttempts.stream().map(this::toReplayAttemptView).toList());
    }

    private RootCauseAnalysisView toAnalysisView(RootCauseAnalysis analysis) {
        return new RootCauseAnalysisView(
                analysis.getId(),
                analysis.getFailureEventId(),
                analysis.getSummary(),
                analysis.getLikelyCause(),
                analysis.getSuggestedFix(),
                analysis.getConfidence(),
                analysis.getCreatedAt());
    }

    private ReplayAttemptView toReplayAttemptView(ReplayAttempt replayAttempt) {
        return new ReplayAttemptView(
                replayAttempt.getId(),
                replayAttempt.getFailureEventId(),
                replayAttempt.getStatusCode(),
                replayAttempt.getResponseHeadersJson(),
                replayAttempt.getResponseBody(),
                replayAttempt.getLatencyMs(),
                replayAttempt.getReplayedAt());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
