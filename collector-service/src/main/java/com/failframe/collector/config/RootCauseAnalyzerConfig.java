package com.failframe.collector.config;

import com.failframe.collector.service.MockRootCauseAnalyzer;
import com.failframe.collector.service.OpenAiRootCauseAnalyzer;
import com.failframe.collector.service.RootCauseAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FailFrameAiProperties.class)
public class RootCauseAnalyzerConfig {

    @Bean
    RootCauseAnalyzer rootCauseAnalyzer(
            FailFrameAiProperties properties,
            Environment environment,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder) {
        String apiKey = environment.getProperty("OPENAI_API_KEY");
        if (properties.provider() != FailFrameAiProperties.Provider.OPENAI || apiKey == null || apiKey.isBlank()) {
            return new MockRootCauseAnalyzer();
        }

        RestClient restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        return new OpenAiRootCauseAnalyzer(properties, objectMapper, restClient, new MockRootCauseAnalyzer());
    }
}
