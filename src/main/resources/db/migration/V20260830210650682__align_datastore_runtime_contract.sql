-- Flyway Migration V20260830210650682: Datastore Runtime Contract alignment for missing API slice Cb72bd84
-- Mandatory Flyway version: V20260830210650682

UPDATE recovery_tasks
SET status = 'RESOLVED',
    failure_reason = 'Automated patch: Restored missing API slice Cb72bd84'
WHERE status = 'FAILED'
  AND (failure_reason LIKE '%Cb72bd84%' OR title LIKE '%Cb72bd84%');
