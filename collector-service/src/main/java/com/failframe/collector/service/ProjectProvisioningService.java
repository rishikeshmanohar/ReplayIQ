package com.failframe.collector.service;

import com.failframe.collector.api.dto.ProjectApiKeyResponse;
import com.failframe.collector.domain.Project;
import com.failframe.collector.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectProvisioningService {

    private final ProjectRepository projectRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final ApiKeyHashService apiKeyHashService;

    public ProjectProvisioningService(
            ProjectRepository projectRepository,
            ApiKeyGenerator apiKeyGenerator,
            ApiKeyHashService apiKeyHashService) {
        this.projectRepository = projectRepository;
        this.apiKeyGenerator = apiKeyGenerator;
        this.apiKeyHashService = apiKeyHashService;
    }

    @Transactional
    public ProjectApiKeyResponse createProjectWithApiKey(String projectName) {
        String apiKey = apiKeyGenerator.generateLocalKey();

        Project project = new Project();
        project.setName(projectName);
        project.setApiKeyHash(apiKeyHashService.hash(apiKey));

        Project saved = projectRepository.save(project);
        return new ProjectApiKeyResponse(saved.getId(), saved.getName(), apiKey, saved.getCreatedAt());
    }
}
