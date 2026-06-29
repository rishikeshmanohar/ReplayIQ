package com.failframe.collector.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.failframe.collector.service.MockRootCauseAnalyzer;
import com.failframe.collector.service.OpenAiRootCauseAnalyzer;
import com.failframe.collector.service.RootCauseAnalyzer;
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
                .withPropertyValues("failframe.ai.provider=openai")
                .run(context -> {
                    assertThat(context).hasSingleBean(RootCauseAnalyzer.class);
                    assertThat(context.getBean(RootCauseAnalyzer.class)).isInstanceOf(MockRootCauseAnalyzer.class);
                });
    }

    @Test
    void createsOpenAiAnalyzerWhenProviderAndApiKeyArePresent() {
        contextRunner
                .withPropertyValues(
                        "failframe.ai.provider=openai",
                        "OPENAI_API_KEY=test-key")
                .run(context -> {
                    assertThat(context).hasSingleBean(RootCauseAnalyzer.class);
                    assertThat(context.getBean(RootCauseAnalyzer.class)).isInstanceOf(OpenAiRootCauseAnalyzer.class);
                });
    }
}
