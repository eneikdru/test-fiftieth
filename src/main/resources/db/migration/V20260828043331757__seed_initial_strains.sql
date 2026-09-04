-- Flyway Migration V20260828043331757: Provide initial epidemiological domain content
-- Mandatory Flyway version: V20260828043331757

CREATE TABLE IF NOT EXISTS strains (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    identified_date DATE,
    origin_country VARCHAR(100),
    severity_level VARCHAR(50)
);

INSERT INTO strains (id, name, description, identified_date, origin_country, severity_level)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'Strain Alpha', 'Initial variant detected', '2023-01-15', 'Unknown', 'HIGH'),
    ('20000000-0000-0000-0000-000000000002', 'Strain Beta', 'Variant with increased transmissibility', '2023-05-20', 'Unknown', 'MODERATE')
ON CONFLICT (id) DO NOTHING;
