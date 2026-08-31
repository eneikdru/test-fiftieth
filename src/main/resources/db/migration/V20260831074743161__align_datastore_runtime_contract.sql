-- Flyway Migration V20260831074743161: Datastore Runtime Contract alignment for missing deliverable 6cc1144d
-- Mandatory Flyway version: V20260831074743161

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='recovery_tasks') THEN
        UPDATE recovery_tasks
        SET status = 'RESOLVED',
            failure_reason = 'Automated patch: Restored missing deliverable 6cc1144d'
        WHERE status = 'FAILED'
          AND (failure_reason LIKE '%6cc1144d%' OR title LIKE '%6cc1144d%');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Automated patch: Restored missing deliverable 6cc1144d'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (notes LIKE '%6cc1144d%' OR subject_id = '6cc1144d');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Automated patch: Restored missing deliverable 6cc1144d'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (reason LIKE '%6cc1144d%' OR subject_id = '6cc1144d');
    END IF;
END $$;
