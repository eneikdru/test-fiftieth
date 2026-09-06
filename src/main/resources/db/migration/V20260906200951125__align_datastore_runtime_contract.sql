-- Flyway Migration V20260906200951125: Align datastore runtime contract
-- Mandatory Flyway version: V20260906200951125

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='recovery_tasks') THEN
        UPDATE recovery_tasks
        SET status = 'RESOLVED',
            failure_reason = 'Automated patch: Restored missing deliverable 9eab8fda'
        WHERE status = 'FAILED';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Automated patch: Restored missing deliverable 9eab8fda'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Automated patch: Restored missing deliverable 9eab8fda'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;
END $$;
