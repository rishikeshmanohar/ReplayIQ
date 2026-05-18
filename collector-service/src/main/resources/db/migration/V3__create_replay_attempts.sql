CREATE TABLE replay_attempts (
    id BIGSERIAL PRIMARY KEY,
    failure_event_id BIGINT NOT NULL REFERENCES api_failure_events (id) ON DELETE CASCADE,
    status_code INTEGER NOT NULL,
    response_headers_json JSONB,
    response_body TEXT,
    latency_ms BIGINT,
    replayed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_replay_attempts_failure_event_id
    ON replay_attempts (failure_event_id);
