ALTER TABLE users ADD COLUMN IF NOT EXISTS moodle_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS moodle_external_id VARCHAR(255);

CREATE TABLE IF NOT EXISTS moodle_role_mappings (
    id BIGSERIAL PRIMARY KEY,
    moodle_role_pattern VARCHAR(255) NOT NULL UNIQUE,
    internal_role VARCHAR(50) NOT NULL
);

INSERT INTO moodle_role_mappings (moodle_role_pattern, internal_role) VALUES
('администратор', 'ADMIN'),
('старший научный сотрудник', 'EPIDEMIOLOGIST'),
('эпидемиолог', 'EPIDEMIOLOGIST'),
('исследователь', 'RESEARCHER'),
('аспирант', 'RESEARCHER')
ON CONFLICT DO NOTHING;
