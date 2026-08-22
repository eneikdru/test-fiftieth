CREATE TABLE user_profiles (
    subject_id VARCHAR(64) NOT NULL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    organization VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE data_export_requests (
    request_id UUID NOT NULL PRIMARY KEY,
    subject_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_format VARCHAR(16) NOT NULL,
    notes TEXT,
    download_url VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    error_code VARCHAR(64),
    error_message TEXT
);

CREATE TABLE data_erasure_requests (
    request_id UUID NOT NULL PRIMARY KEY,
    subject_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    confirmation_token VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    erasure_scope VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    records_erased_count INT,
    error_code VARCHAR(64),
    error_message TEXT
);
