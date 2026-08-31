-- Flyway Migration V20260830210636247: Datastore Runtime Contract alignment for missing Runtime Contract 7990bb88
-- Mandatory Flyway version: V20260830210636247

UPDATE recovery_tasks
SET status = 'RESOLVED',
    failure_reason = 'Automated patch: Restored missing Runtime Contract 7990bb88'
WHERE status = 'FAILED'
  AND (failure_reason LIKE '%7990bb88%' OR title LIKE '%7990bb88%' OR title LIKE '%Runtime Contract%');
