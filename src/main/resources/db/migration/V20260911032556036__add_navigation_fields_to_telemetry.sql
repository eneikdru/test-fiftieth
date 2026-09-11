ALTER TABLE telemetry_events ADD COLUMN module VARCHAR(128);
ALTER TABLE telemetry_events ADD COLUMN title VARCHAR(255);
ALTER TABLE telemetry_events ADD COLUMN success BOOLEAN;
