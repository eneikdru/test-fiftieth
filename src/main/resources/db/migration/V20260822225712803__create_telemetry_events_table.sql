CREATE TABLE telemetry_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    document_id VARCHAR(128),
    search_query VARCHAR(512),
    user_id VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_telemetry_events_event_type ON telemetry_events(event_type);
