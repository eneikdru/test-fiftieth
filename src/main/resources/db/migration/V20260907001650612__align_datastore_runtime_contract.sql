-- Flyway Migration V20260907001650612: Align datastore runtime contract for strains schema
-- Mandatory Flyway version: V20260907001650612

ALTER TABLE strains ADD COLUMN IF NOT EXISTS access_department VARCHAR(255);
ALTER TABLE strains ADD COLUMN IF NOT EXISTS access_course VARCHAR(255);
