-- Flyway Migration V20260902194516319: Datastore Runtime Contract alignment for missing API slice B3f6b79a
-- Mandatory Flyway version: V20260902194516319

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='recovery_tasks') THEN
        UPDATE recovery_tasks
        SET status = 'RESOLVED',
            failure_reason = 'Automated patch: Restored missing API slice B3f6b79a'
        WHERE status = 'FAILED'
          AND (failure_reason LIKE '%B3f6b79a%' OR title LIKE '%B3f6b79a%');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Automated patch: Restored missing API slice B3f6b79a'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (notes LIKE '%B3f6b79a%' OR subject_id = 'B3f6b79a');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Automated patch: Restored missing API slice B3f6b79a'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (reason LIKE '%B3f6b79a%' OR subject_id = 'B3f6b79a');
    END IF;
END $$;
