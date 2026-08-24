-- Flyway Migration V20260824195332838: Create root cause patterns and design review concerns tables for categorization service
-- Mandatory version: V20260824195332838

CREATE TABLE IF NOT EXISTS root_cause_patterns (
    id VARCHAR(64) PRIMARY KEY,
    pattern_name VARCHAR(128) NOT NULL,
    stream_name VARCHAR(64) NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    invariant_pattern_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS design_review_concerns (
    id VARCHAR(64) PRIMARY KEY,
    stream_name VARCHAR(64) NOT NULL,
    epic_sequence INT NOT NULL,
    u_value NUMERIC(10,4) NOT NULL,
    root_cause_pattern_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO root_cause_patterns (id, pattern_name, stream_name, rule_code, invariant_pattern_id, created_at)
VALUES ('RCP-001', 'Review Concerns Out of Control - 8 Consecutive Same Side', 'reviewConcerns', 'WESTERN_ELECTRIC_8_CONSECUTIVE_SAME_SIDE', 'RCP-REVIEW-CONCERNS-001', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
