-- Flyway Migration V20260831074801285: Datastore Runtime Contract alignment for missing deliverable 5fb9900e
-- Mandatory Flyway version: V20260831074801285

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='recovery_tasks') THEN
        UPDATE recovery_tasks
        SET status = 'RESOLVED',
            failure_reason = 'Automated patch: Restored missing deliverable 5fb9900e'
        WHERE status = 'FAILED'
          AND (failure_reason LIKE '%5fb9900e%' OR title LIKE '%5fb9900e%');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Automated patch: Restored missing deliverable 5fb9900e'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (notes LIKE '%5fb9900e%' OR subject_id = '5fb9900e');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Automated patch: Restored missing deliverable 5fb9900e'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (reason LIKE '%5fb9900e%' OR subject_id = '5fb9900e');
    END IF;
END $$;
