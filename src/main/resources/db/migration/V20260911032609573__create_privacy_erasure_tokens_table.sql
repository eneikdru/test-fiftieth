CREATE TABLE privacy_erasure_tokens (
    id BIGSERIAL PRIMARY KEY,
    subject_id VARCHAR(100) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN DEFAULT FALSE NOT NULL
);
CREATE INDEX idx_erasure_tokens_token ON privacy_erasure_tokens(token);
CREATE INDEX idx_erasure_tokens_subject ON privacy_erasure_tokens(subject_id);