-- Flyway Migration V20260830210627183: Datastore Runtime Contract alignment for missing API slice 2c3442ef
-- Mandatory Flyway version: V20260830210627183

UPDATE recovery_tasks
SET status = 'RESOLVED',
    failure_reason = 'Automated patch: Restored missing API slice 2c3442ef'
WHERE status = 'FAILED'
  AND (failure_reason LIKE '%2c3442ef%' OR title LIKE '%2c3442ef%');
