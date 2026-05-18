package com.debugflow.sdk;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

final class BodyCapture {

    static final int MAX_BODY_BYTES = 10 * 1024;

    private BodyCapture() {
    }

    static String fromBytes(byte[] bytes, String encoding) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        int length = Math.min(bytes.length, MAX_BODY_BYTES);
        return new String(Arrays.copyOf(bytes, length), charset(encoding));
    }

    private static Charset charset(String encoding) {
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (RuntimeException ignored) {
            return StandardCharsets.UTF_8;
        }
    }
}
