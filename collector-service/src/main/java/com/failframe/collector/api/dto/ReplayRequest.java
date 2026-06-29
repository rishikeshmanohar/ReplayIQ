package com.failframe.collector.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ReplayRequest(
        @NotBlank String targetBaseUrl,
        Boolean allowPaymentReplay) {
}
