-- Flyway Migration V20260828075430517: Datastore Runtime Contract alignment
CREATE TABLE background_processes (
    id UUID PRIMARY KEY,
    subject_id VARCHAR(100),
    title VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO background_processes (id, subject_id, title, status, failure_reason, created_at, updated_at)
VALUES
('5421d1f0-ec82-43a9-ad0c-9a94345450af', '5421d1f0-ec82-43a9-ad0c-9a94345450af', 'API Slice D3a7a0f6', 'FAILED', 'data_processing_error', NOW(), NOW()),
('8bd0dbae-41f6-466a-95a7-aff680ed0866', '8bd0dbae-41f6-466a-95a7-aff680ed0866', 'Runtime Contract 9b58412d', 'FAILED', 'data_processing_error', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET status = 'FAILED';
