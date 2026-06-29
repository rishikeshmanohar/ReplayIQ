package com.failframe.collector.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PayloadSizeLimiterTest {

    private final PayloadSizeLimiter limiter = new PayloadSizeLimiter();

    @Test
    void keepsNullAndSmallBodies() {
        assertThat(limiter.limit(null)).isNull();
        assertThat(limiter.limit("small body")).isEqualTo("small body");
    }

    @Test
    void truncatesBodiesToTenKilobytes() {
        String body = "a".repeat(PayloadSizeLimiter.MAX_BODY_BYTES + 100);

        String limited = limiter.limit(body);

        assertThat(limited.getBytes(StandardCharsets.UTF_8)).hasSize(PayloadSizeLimiter.MAX_BODY_BYTES);
    }

    @Test
    void doesNotSplitUtf8CodePoints() {
        String multiByteCharacter = "\u00E9";
        String body = "a".repeat(PayloadSizeLimiter.MAX_BODY_BYTES - 1) + multiByteCharacter;

        String limited = limiter.limit(body);

        assertThat(limited).endsWith("a");
        assertThat(limited).doesNotContain(multiByteCharacter);
        assertThat(limited.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(PayloadSizeLimiter.MAX_BODY_BYTES);
    }
}
