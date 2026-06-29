package com.failframe.collector.api;

import com.failframe.collector.api.dto.FailureEventFilter;
import com.failframe.collector.api.dto.FailureIngestionRequest;
import com.failframe.collector.api.dto.FailureIngestionResponse;
import com.failframe.collector.api.dto.FailureIngestionView;
import com.failframe.collector.api.dto.ReplayAttemptView;
import com.failframe.collector.api.dto.ReplayRequest;
import com.failframe.collector.api.dto.RootCauseAnalysisView;
import com.failframe.collector.service.FailureIngestionService;
import com.failframe.collector.service.FailureReplayService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events/failures")
public class FailureIngestionController {

    private static final String API_KEY_HEADER = "X-FailFrame-Api-Key";

    private final FailureIngestionService failureIngestionService;
    private final FailureReplayService failureReplayService;

    public FailureIngestionController(FailureIngestionService failureIngestionService, FailureReplayService failureReplayService) {
        this.failureIngestionService = failureIngestionService;
        this.failureReplayService = failureReplayService;
    }

    @PostMapping
    public ResponseEntity<FailureIngestionResponse> ingest(
            @RequestHeader(name = API_KEY_HEADER, required = false) String apiKey,
            @RequestHeader(name = "traceparent", required = false) String traceparent,
            @Valid @RequestBody FailureIngestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(failureIngestionService.ingest(apiKey, traceparent, request));
    }

    @GetMapping
    public List<FailureIngestionView> list(
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {
        FailureEventFilter filter = new FailureEventFilter(statusCode, serviceName, environment, fromDate, toDate);
        return failureIngestionService.list(filter);
    }

    @GetMapping("/{id}")
    public FailureIngestionView get(@PathVariable Long id) {
        return failureIngestionService.get(id);
    }

    @PostMapping("/{id}/analyze")
    public RootCauseAnalysisView analyze(@PathVariable Long id) {
        return failureIngestionService.analyze(id);
    }

    @PostMapping("/{id}/replay")
    public ReplayAttemptView replay(
            @PathVariable Long id,
            @Valid @RequestBody ReplayRequest request) {
        return failureReplayService.replay(id, request);
    }
}
