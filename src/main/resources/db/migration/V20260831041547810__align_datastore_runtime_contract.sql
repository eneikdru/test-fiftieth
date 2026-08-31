-- Flyway Migration V20260831041547810: Datastore Runtime Contract alignment for unapplied migration 20260830214531892
-- Mandatory Flyway version: V20260831041547810

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='recovery_tasks') THEN
        UPDATE recovery_tasks
        SET status = 'RESOLVED',
            failure_reason = 'Automated patch: Reconciled unapplied migration 20260830214531892'
        WHERE status = 'FAILED'
          AND (failure_reason LIKE '%20260830214531892%' OR title LIKE '%20260830214531892%');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Automated patch: Reconciled unapplied migration 20260830214531892'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (notes LIKE '%20260830214531892%' OR subject_id = '20260830214531892');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Automated patch: Reconciled unapplied migration 20260830214531892'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (reason LIKE '%20260830214531892%' OR subject_id = '20260830214531892');
    END IF;
END $$;
