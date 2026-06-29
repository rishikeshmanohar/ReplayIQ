package com.failframe.collector.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "replay_attempts",
        indexes = {
                @Index(name = "idx_replay_attempts_failure_event_id", columnList = "failure_event_id")
        })
public class ReplayAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long failureEventId;

    @Column(nullable = false)
    private Integer statusCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode responseHeadersJson;

    @Column(columnDefinition = "text")
    private String responseBody;

    private Long latencyMs;

    @Column(nullable = false, updatable = false)
    private Instant replayedAt;

    @PrePersist
    void onCreate() {
        if (replayedAt == null) {
            replayedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getFailureEventId() {
        return failureEventId;
    }

    public void setFailureEventId(Long failureEventId) {
        this.failureEventId = failureEventId;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public JsonNode getResponseHeadersJson() {
        return responseHeadersJson;
    }

    public void setResponseHeadersJson(JsonNode responseHeadersJson) {
        this.responseHeadersJson = responseHeadersJson;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Instant getReplayedAt() {
        return replayedAt;
    }

    public void setReplayedAt(Instant replayedAt) {
        this.replayedAt = replayedAt;
    }
}
