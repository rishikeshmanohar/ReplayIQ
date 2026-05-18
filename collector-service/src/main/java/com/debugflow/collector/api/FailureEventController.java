package com.debugflow.collector.api;

import com.debugflow.collector.api.dto.FailureEventRequest;
import com.debugflow.collector.api.dto.FailureEventResponse;
import com.debugflow.collector.api.dto.RootCauseAnalysisResponse;
import com.debugflow.collector.service.FailureEventService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/failures")
public class FailureEventController {

    private final FailureEventService failureEventService;

    public FailureEventController(FailureEventService failureEventService) {
        this.failureEventService = failureEventService;
    }

    @PostMapping
    public ResponseEntity<FailureEventResponse> capture(@Valid @RequestBody FailureEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(failureEventService.capture(request));
    }

    @GetMapping
    public List<FailureEventResponse> listRecent() {
        return failureEventService.listRecent();
    }

    @GetMapping("/{id}")
    public FailureEventResponse getById(@PathVariable Long id) {
        return failureEventService.getById(id);
    }

    @PostMapping("/{id}/analysis")
    public RootCauseAnalysisResponse analyze(@PathVariable Long id) {
        return failureEventService.analyze(id);
    }
}
