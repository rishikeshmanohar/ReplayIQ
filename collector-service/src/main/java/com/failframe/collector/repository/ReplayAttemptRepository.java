package com.failframe.collector.repository;

import com.failframe.collector.domain.ReplayAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplayAttemptRepository extends JpaRepository<ReplayAttempt, Long> {

    List<ReplayAttempt> findByFailureEventIdOrderByReplayedAtDesc(Long failureEventId);
}
