CREATE TABLE IF NOT EXISTS features (
    id VARCHAR(255) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    origin_feature_id VARCHAR(255),
    dismissed_at TIMESTAMP WITH TIME ZONE,
    valueless BOOLEAN NOT NULL DEFAULT FALSE
);
