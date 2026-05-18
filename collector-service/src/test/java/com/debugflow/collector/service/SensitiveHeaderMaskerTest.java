package com.debugflow.collector.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SensitiveHeaderMaskerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SensitiveHeaderMasker masker = new SensitiveHeaderMasker();

    @Test
    void masksSensitiveHeadersCaseInsensitively() throws Exception {
        JsonNode headers = objectMapper.readTree("""
                {
                  "Authorization": "Bearer secret",
                  "cookie": "session=secret",
                  "Set-Cookie": ["session=secret"],
                  "X-Api-Key": "secret",
                  "X-DebugFlow-Api-Key": "secret",
                  "Content-Type": "application/json"
                }
                """);

        JsonNode masked = masker.mask(headers);

        assertThat(masked.get("Authorization").asText()).isEqualTo("[masked]");
        assertThat(masked.get("cookie").asText()).isEqualTo("[masked]");
        assertThat(masked.get("Set-Cookie").asText()).isEqualTo("[masked]");
        assertThat(masked.get("X-Api-Key").asText()).isEqualTo("[masked]");
        assertThat(masked.get("X-DebugFlow-Api-Key").asText()).isEqualTo("[masked]");
        assertThat(masked.get("Content-Type").asText()).isEqualTo("application/json");
    }

    @Test
    void leavesOriginalHeadersUnchanged() throws Exception {
        JsonNode headers = objectMapper.readTree("{\"Authorization\":\"Bearer secret\"}");

        masker.mask(headers);

        assertThat(headers.get("Authorization").asText()).isEqualTo("Bearer secret");
    }

    @Test
    void ignoresNonObjectHeaders() throws Exception {
        JsonNode headers = objectMapper.readTree("[\"Authorization\"]");

        assertThat(masker.mask(headers)).isSameAs(headers);
        assertThat(masker.mask(null)).isNull();
    }
}
