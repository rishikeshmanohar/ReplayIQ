package com.failframe.collector.service;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class PayloadSizeLimiter {

    public static final int MAX_BODY_BYTES = 10 * 1024;

    public String limit(String payload) {
        if (payload == null || payload.getBytes(StandardCharsets.UTF_8).length <= MAX_BODY_BYTES) {
            return payload;
        }

        StringBuilder limited = new StringBuilder();
        int bytes = 0;
        for (int offset = 0; offset < payload.length(); ) {
            int codePoint = payload.codePointAt(offset);
            String current = new String(Character.toChars(codePoint));
            int currentBytes = current.getBytes(StandardCharsets.UTF_8).length;
            if (bytes + currentBytes > MAX_BODY_BYTES) {
                break;
            }
            limited.append(current);
            bytes += currentBytes;
            offset += Character.charCount(codePoint);
        }
        return limited.toString();
    }
}
