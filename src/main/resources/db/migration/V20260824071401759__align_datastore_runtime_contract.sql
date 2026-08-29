-- Flyway Migration V20260824071401759: Finalize pending privacy requests
-- Domain: Data Subject Rights (GDPR)

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Reconciled merged PR for task with null session PR URL'
WHERE subject_id IN ('5421d1f0-ec82-43a9-ad0c-9a94345450af', '8bd0dbae-41f6-466a-95a7-aff680ed0866')
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Reconciled merged PR for task with null session PR URL'
WHERE subject_id IN ('5421d1f0-ec82-43a9-ad0c-9a94345450af', '8bd0dbae-41f6-466a-95a7-aff680ed0866')
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
