CREATE TABLE IF NOT EXISTS dossier_reports (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(100) NOT NULL,
    template_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    summary_text TEXT,
    document_count INT NOT NULL DEFAULT 0,
    download_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_dossier_reports_employee_id ON dossier_reports(employee_id);
