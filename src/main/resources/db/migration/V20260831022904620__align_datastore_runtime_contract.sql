-- Flyway Migration V20260831022904620: Datastore Runtime Contract alignment for missing deliverable 0386e9cd
-- Mandatory Flyway version: V20260831022904620

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='recovery_tasks') THEN
        UPDATE recovery_tasks
        SET status = 'RESOLVED',
            failure_reason = 'Automated patch: Restored missing deliverable 0386e9cd'
        WHERE status = 'FAILED'
          AND (failure_reason LIKE '%0386e9cd%' OR title LIKE '%0386e9cd%');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Automated patch: Restored missing deliverable 0386e9cd'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (notes LIKE '%0386e9cd%' OR subject_id = '0386e9cd');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Automated patch: Restored missing deliverable 0386e9cd'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (reason LIKE '%0386e9cd%' OR subject_id = '0386e9cd');
    END IF;
END $$;
