package com.failframe.collector.repository;

import com.failframe.collector.domain.ApiFailureEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApiFailureEventRepository extends JpaRepository<ApiFailureEvent, Long>, JpaSpecificationExecutor<ApiFailureEvent> {

    List<ApiFailureEvent> findTop50ByOrderByOccurredAtDesc();

    List<ApiFailureEvent> findTop50ByProjectIdOrderByOccurredAtDesc(Long projectId);

    Optional<ApiFailureEvent> findByIdAndProjectId(Long id, Long projectId);
}
