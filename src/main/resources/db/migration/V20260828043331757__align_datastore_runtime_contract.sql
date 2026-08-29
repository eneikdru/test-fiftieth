-- Flyway Migration V20260828043331757: Datastore Runtime Contract alignment for missing API slice and runtime contract
-- Mandatory Flyway version: V20260828043331757

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Automated patch: Restored missing API slice D3a7a0f6 and runtime contract 9b58412d'
WHERE subject_id IN ('5421d1f0-ec82-43a9-ad0c-9a94345450af', '8bd0dbae-41f6-466a-95a7-aff680ed0866')
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Automated patch: Restored missing API slice D3a7a0f6 and runtime contract 9b58412d'
WHERE subject_id IN ('5421d1f0-ec82-43a9-ad0c-9a94345450af', '8bd0dbae-41f6-466a-95a7-aff680ed0866')
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
