ALTER TABLE users ADD COLUMN IF NOT EXISTS courses VARCHAR(500);
ALTER TABLE employee_documents ADD COLUMN IF NOT EXISTS access_department VARCHAR(255);
ALTER TABLE employee_documents ADD COLUMN IF NOT EXISTS access_course VARCHAR(255);
ALTER TABLE dossier_reports ADD COLUMN IF NOT EXISTS access_department VARCHAR(255);
ALTER TABLE dossier_reports ADD COLUMN IF NOT EXISTS access_course VARCHAR(255);
