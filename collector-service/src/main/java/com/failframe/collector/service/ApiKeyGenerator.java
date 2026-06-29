package com.failframe.collector.service;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyGenerator {

    private static final int KEY_BYTES = 32;
    private static final String LOCAL_PREFIX = "ff_local_";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateLocalKey() {
        byte[] bytes = new byte[KEY_BYTES];
        secureRandom.nextBytes(bytes);
        return LOCAL_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
