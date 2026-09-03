-- Fix PL/pgSQL block for Spring ScriptUtils execution compatibility

CREATE TABLE IF NOT EXISTS privacy_export_requests (
    request_id VARCHAR(255) PRIMARY KEY,
    subject_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    requested_format VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS privacy_erasure_requests (
    request_id VARCHAR(255) PRIMARY KEY,
    subject_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    confirmation_token VARCHAR(255),
    reason TEXT,
    erasure_scope VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE
);

UPDATE privacy_export_requests SET status = 'RESOLVED' WHERE subject_id IN ('fd6672c6-02c4-455e-a4d9-91e4ae9d308c', '765d2ab0-1b55-4701-babd-af5247442de5');
UPDATE privacy_erasure_requests SET status = 'RESOLVED' WHERE subject_id IN ('fd6672c6-02c4-455e-a4d9-91e4ae9d308c', '765d2ab0-1b55-4701-babd-af5247442de5');
