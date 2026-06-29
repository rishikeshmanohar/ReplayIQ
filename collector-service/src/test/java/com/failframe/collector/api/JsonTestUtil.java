package com.failframe.collector.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MvcResult;

final class JsonTestUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonTestUtil() {
    }

    static Long longValue(MvcResult result, String fieldName) throws Exception {
        JsonNode body = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString());
        return body.get(fieldName).asLong();
    }

    static String quote(String value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not quote test value", ex);
        }
    }
}
