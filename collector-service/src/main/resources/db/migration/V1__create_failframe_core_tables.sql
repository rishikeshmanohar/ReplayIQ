CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    api_key_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_projects_api_key_hash
    ON projects (api_key_hash);

CREATE TABLE service_apps (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    environment VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_service_apps_project_id
    ON service_apps (project_id);

CREATE TABLE api_failure_events (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    service_name VARCHAR(255) NOT NULL,
    environment VARCHAR(255),
    trace_id VARCHAR(255),
    span_id VARCHAR(255),
    http_method VARCHAR(16) NOT NULL,
    path VARCHAR(2048) NOT NULL,
    query_string TEXT,
    status_code INTEGER NOT NULL,
    latency_ms BIGINT,
    exception_type VARCHAR(255),
    exception_message TEXT,
    stack_trace TEXT,
    request_headers_json JSONB,
    request_body TEXT,
    response_headers_json JSONB,
    response_body TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_api_failure_events_project_id
    ON api_failure_events (project_id);

CREATE INDEX idx_api_failure_events_trace_id
    ON api_failure_events (trace_id);

CREATE INDEX idx_api_failure_events_status_code
    ON api_failure_events (status_code);

CREATE INDEX idx_api_failure_events_occurred_at
    ON api_failure_events (occurred_at);

CREATE INDEX idx_api_failure_events_service_name
    ON api_failure_events (service_name);

CREATE TABLE root_cause_analyses (
    id BIGSERIAL PRIMARY KEY,
    failure_event_id BIGINT NOT NULL REFERENCES api_failure_events (id) ON DELETE CASCADE,
    summary TEXT NOT NULL,
    likely_cause TEXT,
    suggested_fix TEXT,
    confidence DOUBLE PRECISION,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_root_cause_analyses_failure_event_id
    ON root_cause_analyses (failure_event_id);

INSERT INTO projects (id, name, api_key_hash)
VALUES (1, 'Local Development', 'e07bc6524d40a0c7ca8789206007d05ef7f8195850b5f4389b93c5d27e571033')
ON CONFLICT (api_key_hash) DO NOTHING;

SELECT setval(pg_get_serial_sequence('projects', 'id'), GREATEST((SELECT MAX(id) FROM projects), 1));
