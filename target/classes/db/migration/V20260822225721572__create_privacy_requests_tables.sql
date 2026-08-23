CREATE TABLE privacy_export_requests (
    request_id VARCHAR(36) PRIMARY KEY,
    subject_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_format VARCHAR(20) NOT NULL,
    download_url VARCHAR(255),
    notes VARCHAR(500),
    export_payload TEXT,
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_export_requests_subject ON privacy_export_requests(subject_id);
CREATE INDEX idx_export_requests_status ON privacy_export_requests(status);

CREATE TABLE privacy_erasure_requests (
    request_id VARCHAR(36) PRIMARY KEY,
    subject_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    confirmation_token VARCHAR(255) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    erasure_scope VARCHAR(50) NOT NULL,
    records_erased_count INT,
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_erasure_requests_subject ON privacy_erasure_requests(subject_id);
CREATE INDEX idx_erasure_requests_status ON privacy_erasure_requests(status);
