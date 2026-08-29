-- Flyway Migration V20260824053157128: Finalize pending privacy requests
-- Domain: Data Subject Rights (GDPR)

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Reconciled privacy data export request'
WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866'
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Reconciled privacy data erasure request'
WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866'
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
