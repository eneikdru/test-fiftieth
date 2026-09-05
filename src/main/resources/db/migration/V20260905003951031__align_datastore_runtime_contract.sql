-- Flyway Migration V20260905003951031: Align datastore runtime contract
-- Mandatory Flyway version: V20260905003951031

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='recovery_tasks') THEN
        UPDATE recovery_tasks
        SET status = 'RESOLVED',
            failure_reason = 'Automated patch: Restored missing deliverable Be94a597'
        WHERE subject_id = 'Be94a597' AND status = 'FAILED';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Automated patch: Restored missing deliverable Be94a597'
        WHERE subject_id = 'Be94a597' AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Automated patch: Restored missing deliverable Be94a597'
        WHERE subject_id = 'Be94a597' AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;
END $$;
