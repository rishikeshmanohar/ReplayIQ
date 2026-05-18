package com.debugflow.sdk;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class DebugFlowClient {

    private static final String FAILURE_EVENTS_PATH = "/api/v1/events/failures";

    private final WebClient webClient;
    private final DebugFlowProperties properties;

    public DebugFlowClient(WebClient webClient, DebugFlowProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public void capture(DebugFlowEvent event) {
        if (!properties.isEnabled() || !properties.hasApiKey()) {
            return;
        }

        try {
            webClient.post()
                .uri(FAILURE_EVENTS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .onErrorResume(ignored -> Mono.empty())
                .subscribe();
        } catch (RuntimeException ignored) {
            // Application traffic must never fail because DebugFlow is unavailable.
        }
    }
}
