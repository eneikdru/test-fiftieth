-- Flyway Migration V20260830210646280: Datastore Runtime Contract alignment for missing Runtime Contract 493ab311
-- Mandatory Flyway version: V20260830210646280

UPDATE recovery_tasks
SET status = 'RESOLVED',
    failure_reason = 'Automated patch: Restored missing Runtime Contract 493ab311'
WHERE status = 'FAILED'
  AND (failure_reason LIKE '%493ab311%' OR title LIKE '%493ab311%' OR title LIKE '%Runtime Contract%');
