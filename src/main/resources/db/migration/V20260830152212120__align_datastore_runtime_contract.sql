-- Flyway Migration V20260830152212120: Datastore Runtime Contract alignment for missing runtime contract 65921fad
-- Mandatory Flyway version: V20260830152212120

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Automated patch: Restored missing runtime contract 65921fad'
WHERE subject_id IN ('65921fad-ec82-43a9-ad0c-9a94345450af')
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Automated patch: Restored missing runtime contract 65921fad'
WHERE subject_id IN ('65921fad-ec82-43a9-ad0c-9a94345450af')
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
