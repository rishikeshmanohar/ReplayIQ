package com.failframe.collector.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ApiKeyHashServiceTest {

    private final ApiKeyHashService apiKeyHashService = new ApiKeyHashService();

    @Test
    void hashesApiKeysWithSalt() {
        String firstHash = apiKeyHashService.hash("ff_local_secret");
        String secondHash = apiKeyHashService.hash("ff_local_secret");

        assertThat(firstHash).startsWith("sha256:");
        assertThat(secondHash).startsWith("sha256:");
        assertThat(firstHash).isNotEqualTo(secondHash);
        assertThat(apiKeyHashService.matches("ff_local_secret", firstHash)).isTrue();
        assertThat(apiKeyHashService.matches("wrong", firstHash)).isFalse();
    }

    @Test
    void matchesDeterministicSaltedHash() {
        byte[] salt = "fixed-test-salt".getBytes(StandardCharsets.UTF_8);
        String hash = apiKeyHashService.hash("ff_local_secret", salt);

        assertThat(apiKeyHashService.matches("ff_local_secret", hash)).isTrue();
        assertThat(apiKeyHashService.matches("ff_local_secret", "sha256:not-valid:not-valid")).isFalse();
    }

    @Test
    void supportsLegacyUnsaltedSha256DuringMigration() {
        String legacyHash = "e07bc6524d40a0c7ca8789206007d05ef7f8195850b5f4389b93c5d27e571033";

        assertThat(apiKeyHashService.matches("failframe-local-dev-key", legacyHash)).isTrue();
    }
}
