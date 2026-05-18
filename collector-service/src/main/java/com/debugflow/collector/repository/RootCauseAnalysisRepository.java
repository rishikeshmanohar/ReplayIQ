package com.debugflow.collector.repository;

import com.debugflow.collector.domain.RootCauseAnalysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RootCauseAnalysisRepository extends JpaRepository<RootCauseAnalysis, Long> {

    Optional<RootCauseAnalysis> findTopByFailureEventIdOrderByCreatedAtDesc(Long failureEventId);
}
