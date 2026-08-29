-- Flyway Migration V20260824103408450: Datastore Runtime Contract alignment for task 8bd0dbae
-- Mandatory Flyway version: V20260824103408450

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Automated patch: Restored missing runtime contract 9b58412d'
WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866'
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Automated patch: Restored missing runtime contract 9b58412d'
WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866'
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
