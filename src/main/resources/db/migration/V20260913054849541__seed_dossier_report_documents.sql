-- Seed the associative table between dossier_reports and employee_documents
-- Mapping report for EMP-007 to its 2 STRAIN_ISOLATION documents
-- Mapping report for EMP-008 to its 1 DOSSIER_ENTRY document

INSERT INTO dossier_report_documents (dossier_report_id, employee_document_id, created_at)
SELECT dr.id, ed.id, CURRENT_TIMESTAMP
FROM dossier_reports dr
JOIN employee_documents ed ON dr.employee_id = ed.employee_id
WHERE (dr.employee_id = 'EMP-007' AND ed.doc_type = 'STRAIN_ISOLATION')
   OR (dr.employee_id = 'EMP-008' AND ed.doc_type = 'DOSSIER_ENTRY')
ON CONFLICT DO NOTHING;
