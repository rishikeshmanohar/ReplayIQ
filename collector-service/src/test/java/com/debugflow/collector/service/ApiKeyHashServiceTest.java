package com.debugflow.collector.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ApiKeyHashServiceTest {

    private final ApiKeyHashService apiKeyHashService = new ApiKeyHashService();

    @Test
    void hashesApiKeysWithSalt() {
        String firstHash = apiKeyHashService.hash("df_local_secret");
        String secondHash = apiKeyHashService.hash("df_local_secret");

        assertThat(firstHash).startsWith("sha256:");
        assertThat(secondHash).startsWith("sha256:");
        assertThat(firstHash).isNotEqualTo(secondHash);
        assertThat(apiKeyHashService.matches("df_local_secret", firstHash)).isTrue();
        assertThat(apiKeyHashService.matches("wrong", firstHash)).isFalse();
    }

    @Test
    void matchesDeterministicSaltedHash() {
        byte[] salt = "fixed-test-salt".getBytes(StandardCharsets.UTF_8);
        String hash = apiKeyHashService.hash("df_local_secret", salt);

        assertThat(apiKeyHashService.matches("df_local_secret", hash)).isTrue();
        assertThat(apiKeyHashService.matches("df_local_secret", "sha256:not-valid:not-valid")).isFalse();
    }

    @Test
    void supportsLegacyUnsaltedSha256DuringMigration() {
        String legacyHash = "75666eca7e48000f9b2b1a58e6fbf6a014187f6b5bf62c6fd1dd3ef46b3b8e80";

        assertThat(apiKeyHashService.matches("debugflow-local-dev-key", legacyHash)).isTrue();
    }
}
