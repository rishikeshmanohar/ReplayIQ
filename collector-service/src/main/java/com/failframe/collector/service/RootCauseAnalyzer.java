package com.failframe.collector.service;

import com.failframe.collector.domain.ApiFailureEvent;

public interface RootCauseAnalyzer {

    RootCauseAnalysisResult analyze(ApiFailureEvent event);
}
