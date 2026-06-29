package com.failframe.sdk;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "failframe")
public record FailFrameProperties(
        Boolean enabled,
        String apiKey,
        String collectorUrl,
        String serviceName,
        String environment) {

    public FailFrameProperties {
        enabled = enabled == null ? Boolean.TRUE : enabled;
        apiKey = apiKey == null ? "" : apiKey;
        collectorUrl = normalizeCollectorUrl(collectorUrl);
        serviceName = isBlank(serviceName) ? "unknown-service" : serviceName;
        environment = isBlank(environment) ? "local" : environment;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public boolean hasApiKey() {
        return !apiKey.isBlank();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeCollectorUrl(String value) {
        String normalized = isBlank(value) ? "http://localhost:8080" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
