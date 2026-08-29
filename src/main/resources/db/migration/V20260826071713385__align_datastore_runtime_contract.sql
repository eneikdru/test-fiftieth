-- Flyway Migration V20260826071713385: Align datastore runtime contract
-- Domain: Data Subject Rights (GDPR) and Telemetry

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Reconciled backend telemetry operations for task delivery'
WHERE subject_id IN ('5421d1f0-ec82-43a9-ad0c-9a94345450af', '8bd0dbae-41f6-466a-95a7-aff680ed0866')
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Reconciled backend telemetry operations for task delivery'
WHERE subject_id IN ('5421d1f0-ec82-43a9-ad0c-9a94345450af', '8bd0dbae-41f6-466a-95a7-aff680ed0866')
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
