package com.debugflow.collector.api;

import com.debugflow.collector.api.dto.CreateDevProjectRequest;
import com.debugflow.collector.api.dto.ProjectApiKeyResponse;
import com.debugflow.collector.service.ProjectProvisioningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/projects")
public class AdminProjectController {

    private final ProjectProvisioningService projectProvisioningService;

    public AdminProjectController(ProjectProvisioningService projectProvisioningService) {
        this.projectProvisioningService = projectProvisioningService;
    }

    @PostMapping("/dev-key")
    public ResponseEntity<ProjectApiKeyResponse> createDevProject(@RequestBody(required = false) CreateDevProjectRequest request) {
        String projectName = request == null || request.name() == null || request.name().isBlank()
                ? "Local Development"
                : request.name().trim();
        return ResponseEntity.status(HttpStatus.CREATED).body(projectProvisioningService.createProjectWithApiKey(projectName));
    }
}
