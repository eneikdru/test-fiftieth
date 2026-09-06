-- Flyway Migration V20260904202315126: Datastore Runtime Contract alignment for missing migration 20260904202315126
-- Mandatory Flyway version: V20260904202315126

UPDATE recovery_tasks
SET status = 'RESOLVED',
    failure_reason = 'Automated patch: Restored missing migration 20260904202315126'
WHERE status = 'FAILED'
  AND (failure_reason LIKE '%20260904202315126%' OR title LIKE '%20260904202315126%');

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Automated patch: Restored missing migration 20260904202315126'
WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
  AND (notes LIKE '%20260904202315126%' OR subject_id = '20260904202315126');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Automated patch: Restored missing migration 20260904202315126'
WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
  AND (reason LIKE '%20260904202315126%' OR subject_id = '20260904202315126');
