package com.debugflow.collector.repository;

import com.debugflow.collector.domain.ServiceApp;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceAppRepository extends JpaRepository<ServiceApp, Long> {

    List<ServiceApp> findByProjectIdOrderByNameAsc(Long projectId);
}
