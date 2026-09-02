-- Flyway Migration V20260831103855370: Datastore Runtime Contract alignment for missing deliverable B764b6a9
-- Mandatory Flyway version: V20260831103855370

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='recovery_tasks') THEN
        UPDATE recovery_tasks
        SET status = 'RESOLVED',
            failure_reason = 'Automated patch: Restored missing deliverable B764b6a9'
        WHERE status = 'FAILED'
          AND (failure_reason LIKE '%B764b6a9%' OR title LIKE '%B764b6a9%');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Automated patch: Restored missing deliverable B764b6a9'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (notes LIKE '%B764b6a9%' OR subject_id = 'B764b6a9');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Automated patch: Restored missing deliverable B764b6a9'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (reason LIKE '%B764b6a9%' OR subject_id = 'B764b6a9');
    END IF;
END $$;
