-- Flyway Migration V20260906160551904: Create strains table if not exists
-- Mandatory Flyway version: V20260906160551904

CREATE TABLE IF NOT EXISTS strains (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    identified_date DATE,
    origin_country VARCHAR(100),
    severity_level VARCHAR(50)
);
