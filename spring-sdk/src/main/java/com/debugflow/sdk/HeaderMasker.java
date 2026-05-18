package com.debugflow.sdk;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class HeaderMasker {

    private static final String MASKED_VALUE = "[masked]";
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "x-debugflow-api-key");

    public Map<String, List<String>> maskRequestHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, mask(name, Collections.list(request.getHeaders(name))));
        }
        return headers;
    }

    public Map<String, List<String>> maskResponseHeaders(ContentCachingResponseWrapper response) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            headers.put(name, mask(name, new ArrayList<>(response.getHeaders(name))));
        }
        return headers;
    }

    private List<String> mask(String name, List<String> values) {
        if (SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
            return List.of(MASKED_VALUE);
        }
        return values;
    }
}
