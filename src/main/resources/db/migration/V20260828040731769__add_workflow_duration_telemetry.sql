ALTER TABLE telemetry_events ADD COLUMN workflow_duration_ms BIGINT;
ALTER TABLE telemetry_events ADD COLUMN start_time TIMESTAMP WITH TIME ZONE;
ALTER TABLE telemetry_events ADD COLUMN end_time TIMESTAMP WITH TIME ZONE;
