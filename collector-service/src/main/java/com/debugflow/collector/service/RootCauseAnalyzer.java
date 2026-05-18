package com.debugflow.collector.service;

import com.debugflow.collector.domain.ApiFailureEvent;

public interface RootCauseAnalyzer {

    RootCauseAnalysisResult analyze(ApiFailureEvent event);
}
