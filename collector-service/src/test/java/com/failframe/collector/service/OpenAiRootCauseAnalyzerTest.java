package com.failframe.collector.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.failframe.collector.config.FailFrameAiProperties;
import com.failframe.collector.domain.ApiFailureEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenAiRootCauseAnalyzerTest {

    @Test
    void fallsBackToMockWhenOpenAiCallFails() {
        RestClient failingClient = RestClient.builder()
                .requestFactory((uri, httpMethod) -> {
                    throw new IOException("collector test network failure");
                })
                .build();
        OpenAiRootCauseAnalyzer analyzer = new OpenAiRootCauseAnalyzer(
                new FailFrameAiProperties(FailFrameAiProperties.Provider.OPENAI, "https://api.openai.com/v1", "test-model"),
                new ObjectMapper(),
                failingClient,
                new MockRootCauseAnalyzer());

        ApiFailureEvent event = new ApiFailureEvent();
        event.setProjectId(1L);
        event.setServiceName("checkout-service");
        event.setHttpMethod("GET");
        event.setPath("/api/fail/downstream");
        event.setStatusCode(500);
        event.setExceptionMessage("Inventory service timed out");

        RootCauseAnalysisResult result = analyzer.analyze(event);

        assertThat(result.summary()).containsIgnoringCase("timeout");
        assertThat(result.likelyCause()).containsIgnoringCase("downstream");
        assertThat(result.confidence()).isGreaterThan(0.0);
    }
}
