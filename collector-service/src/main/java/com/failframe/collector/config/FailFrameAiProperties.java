package com.failframe.collector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "failframe.ai")
public record FailFrameAiProperties(
        Provider provider,
        String baseUrl,
        String model) {

    public FailFrameAiProperties {
        provider = provider == null ? Provider.MOCK : provider;
        baseUrl = isBlank(baseUrl) ? "https://api.openai.com/v1" : trimTrailingSlash(baseUrl);
        model = isBlank(model) ? "gpt-4o-mini" : model;
    }

    public enum Provider {
        MOCK,
        OPENAI
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
