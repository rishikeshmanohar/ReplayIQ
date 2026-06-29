package com.failframe.sdk;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class FailFrameClient {

    private static final String FAILURE_EVENTS_PATH = "/api/v1/events/failures";

    private final WebClient webClient;
    private final FailFrameProperties properties;

    public FailFrameClient(WebClient webClient, FailFrameProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public void capture(FailFrameEvent event) {
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
            // Application traffic must never fail because FailFrame is unavailable.
        }
    }
}
