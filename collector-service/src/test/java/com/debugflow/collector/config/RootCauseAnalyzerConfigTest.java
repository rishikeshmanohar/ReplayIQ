package com.debugflow.collector.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.debugflow.collector.service.MockRootCauseAnalyzer;
import com.debugflow.collector.service.OpenAiRootCauseAnalyzer;
import com.debugflow.collector.service.RootCauseAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class RootCauseAnalyzerConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RootCauseAnalyzerConfig.class)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void defaultsToMockAnalyzer() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RootCauseAnalyzer.class);
            assertThat(context.getBean(RootCauseAnalyzer.class)).isInstanceOf(MockRootCauseAnalyzer.class);
        });
    }

    @Test
    void fallsBackToMockWhenOpenAiProviderHasNoApiKey() {
        contextRunner
                .withPropertyValues("debugflow.ai.provider=openai")
                .run(context -> {
                    assertThat(context).hasSingleBean(RootCauseAnalyzer.class);
                    assertThat(context.getBean(RootCauseAnalyzer.class)).isInstanceOf(MockRootCauseAnalyzer.class);
                });
    }

    @Test
    void createsOpenAiAnalyzerWhenProviderAndApiKeyArePresent() {
        contextRunner
                .withPropertyValues(
                        "debugflow.ai.provider=openai",
                        "OPENAI_API_KEY=test-key")
                .run(context -> {
                    assertThat(context).hasSingleBean(RootCauseAnalyzer.class);
                    assertThat(context.getBean(RootCauseAnalyzer.class)).isInstanceOf(OpenAiRootCauseAnalyzer.class);
                });
    }
}
