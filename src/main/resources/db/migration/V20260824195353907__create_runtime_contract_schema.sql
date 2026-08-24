-- Mandatory Flyway version: V20260824195353907
-- Creates runtime contract table

CREATE TABLE IF NOT EXISTS runtime_contract_configurations (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
