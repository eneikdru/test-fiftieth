-- Flyway Migration V20260830234309500: Datastore Runtime Contract alignment for missing API slice E7f360ec
-- Mandatory Flyway version: V20260830234309500

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='recovery_tasks') THEN
        UPDATE recovery_tasks
        SET status = 'RESOLVED',
            failure_reason = 'Automated patch: Restored missing API slice E7f360ec'
        WHERE status = 'FAILED'
          AND (failure_reason LIKE '%E7f360ec%' OR title LIKE '%E7f360ec%');
    END IF;
END $$;
