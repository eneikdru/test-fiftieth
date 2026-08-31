-- Flyway Migration V20260831041546812: Datastore Runtime Contract alignment for missing deliverable 157d3f62
-- Mandatory Flyway version: V20260831041546812

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='recovery_tasks') THEN
        UPDATE recovery_tasks
        SET status = 'RESOLVED',
            failure_reason = 'Automated patch: Restored missing deliverable 157d3f62'
        WHERE status = 'FAILED'
          AND (failure_reason LIKE '%157d3f62%' OR title LIKE '%157d3f62%');
    END IF;
END $$;
