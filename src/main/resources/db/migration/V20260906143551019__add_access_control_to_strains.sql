CREATE TABLE IF NOT EXISTS strains (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    identified_date DATE,
    origin_country VARCHAR(100),
    severity_level VARCHAR(50)
);

ALTER TABLE strains ADD COLUMN IF NOT EXISTS access_department VARCHAR(255);
ALTER TABLE strains ADD COLUMN IF NOT EXISTS access_course VARCHAR(255);
