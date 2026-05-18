package com.debugflow.collector.config;

import com.debugflow.collector.service.MockRootCauseAnalyzer;
import com.debugflow.collector.service.OpenAiRootCauseAnalyzer;
import com.debugflow.collector.service.RootCauseAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(DebugFlowAiProperties.class)
public class RootCauseAnalyzerConfig {

    @Bean
    RootCauseAnalyzer rootCauseAnalyzer(
            DebugFlowAiProperties properties,
            Environment environment,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder) {
        String apiKey = environment.getProperty("OPENAI_API_KEY");
        if (properties.provider() != DebugFlowAiProperties.Provider.OPENAI || apiKey == null || apiKey.isBlank()) {
            return new MockRootCauseAnalyzer();
        }

        RestClient restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        return new OpenAiRootCauseAnalyzer(properties, objectMapper, restClient, new MockRootCauseAnalyzer());
    }
}
