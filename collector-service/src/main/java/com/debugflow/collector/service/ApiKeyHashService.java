package com.debugflow.collector.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyHashService {

    private static final String ALGORITHM_PREFIX = "sha256";
    private static final int SALT_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String apiKey) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return hash(apiKey, salt);
    }

    public boolean matches(String apiKey, String storedHash) {
        if (apiKey == null || apiKey.isBlank() || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        String[] parts = storedHash.split(":");
        if (parts.length == 3 && ALGORITHM_PREFIX.equals(parts[0])) {
            try {
                byte[] salt = decode(parts[1]);
                byte[] expectedHash = decode(parts[2]);
                byte[] actualHash = sha256(salt, apiKey.getBytes(StandardCharsets.UTF_8));
                return MessageDigest.isEqual(expectedHash, actualHash);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        return MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8), legacySha256Hex(apiKey).getBytes(StandardCharsets.UTF_8));
    }

    String hash(String apiKey, byte[] salt) {
        byte[] hash = sha256(salt, apiKey.getBytes(StandardCharsets.UTF_8));
        return "%s:%s:%s".formatted(ALGORITHM_PREFIX, encode(salt), encode(hash));
    }

    private byte[] sha256(byte[] salt, byte[] apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(ByteBuffer.allocate(salt.length + apiKey.length).put(salt).put(apiKey).array());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String legacySha256Hex(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(hash.length * 2);
            for (byte current : hash) {
                value.append(String.format("%02x", current & 0xff));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
