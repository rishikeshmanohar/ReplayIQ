package com.failframe.collector.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SensitiveHeaderMasker {

    private static final String MASKED_VALUE = "[masked]";
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "x-failframe-api-key");

    public JsonNode mask(JsonNode headers) {
        if (headers == null || !headers.isObject()) {
            return headers;
        }

        ObjectNode masked = headers.deepCopy();
        masked.fieldNames().forEachRemaining(fieldName -> {
            String normalized = fieldName.toLowerCase(Locale.ROOT);
            if (SENSITIVE_HEADERS.contains(normalized)) {
                masked.put(fieldName, MASKED_VALUE);
            }
        });
        return masked;
    }
}
