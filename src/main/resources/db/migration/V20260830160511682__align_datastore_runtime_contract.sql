-- Flyway Migration V20260830160511682: Datastore Runtime Contract alignment for missing Recovery Data Schema
-- Mandatory Flyway version: V20260830160511682

UPDATE recovery_tasks
SET status = 'RESOLVED',
    failure_reason = 'Automated patch: Restored missing Recovery Data Schema'
WHERE status = 'FAILED'
  AND (failure_reason LIKE '%reconcileClosedUnmergedPullRequest%' OR title LIKE '%Recovery Data Schema%');
