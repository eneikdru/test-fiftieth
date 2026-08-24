ALTER TABLE employee_documents ADD COLUMN IF NOT EXISTS employee_surname VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_employee_docs_surname ON employee_documents(employee_surname);
