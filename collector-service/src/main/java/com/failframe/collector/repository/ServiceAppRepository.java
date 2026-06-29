package com.failframe.collector.repository;

import com.failframe.collector.domain.ServiceApp;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceAppRepository extends JpaRepository<ServiceApp, Long> {

    List<ServiceApp> findByProjectIdOrderByNameAsc(Long projectId);
}
