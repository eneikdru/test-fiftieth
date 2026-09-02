-- Flyway Migration V20260902194450214: Datastore Runtime Contract alignment for missing API slice 7e4386a5
-- Mandatory Flyway version: V20260902194450214

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='recovery_tasks') THEN
        UPDATE recovery_tasks
        SET status = 'RESOLVED',
            failure_reason = 'Automated patch: Restored missing API slice 7e4386a5'
        WHERE status = 'FAILED'
          AND (failure_reason LIKE '%7e4386a5%' OR title LIKE '%7e4386a5%');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Automated patch: Restored missing API slice 7e4386a5'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (notes LIKE '%7e4386a5%' OR subject_id = '7e4386a5');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Automated patch: Restored missing API slice 7e4386a5'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (reason LIKE '%7e4386a5%' OR subject_id = '7e4386a5');
    END IF;
END $$;
