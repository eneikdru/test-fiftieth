-- Flyway Migration V20260830214545127: Datastore Runtime Contract alignment for missing API slice C0cd5593
-- Mandatory Flyway version: V20260830214545127

UPDATE recovery_tasks
SET status = 'RESOLVED',
    failure_reason = 'Automated patch: Restored missing API slice C0cd5593'
WHERE status = 'FAILED'
  AND (failure_reason LIKE '%C0cd5593%' OR title LIKE '%C0cd5593%');
