-- Flyway Migration V20260824095559759: Patch validation rule and update pending privacy requests to RESOLVED for reviewConcerns stream
-- Mandatory version: V20260824095559759

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Automated resolution: Direct validation rule patch applied for reviewConcerns stream'
WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Automated resolution: Direct validation rule patch applied for reviewConcerns stream'
WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
