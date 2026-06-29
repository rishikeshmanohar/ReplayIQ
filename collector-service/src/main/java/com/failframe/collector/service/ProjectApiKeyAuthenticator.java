package com.failframe.collector.service;

import com.failframe.collector.domain.Project;
import com.failframe.collector.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectApiKeyAuthenticator {

    private final ApiKeyHashService apiKeyHashService;
    private final ProjectRepository projectRepository;

    public ProjectApiKeyAuthenticator(ApiKeyHashService apiKeyHashService, ProjectRepository projectRepository) {
        this.apiKeyHashService = apiKeyHashService;
        this.projectRepository = projectRepository;
    }

    public Project authenticate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-FailFrame-Api-Key header");
        }

        return projectRepository.findAll().stream()
                .filter(project -> apiKeyHashService.matches(apiKey, project.getApiKeyHash()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid API key"));
    }
}
