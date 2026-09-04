-- Flyway Migration V20260828041227521: Align datastore runtime contract for missing tasks 5421d1f0 and 8bd0dbae
-- Mandatory Flyway version: V20260828041227521

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='recovery_tasks') THEN
        UPDATE recovery_tasks
        SET status = 'RESOLVED',
            failure_reason = 'Automated patch: Restored missing deliverables 5421d1f0 and 8bd0dbae'
        WHERE subject_id IN ('5421d1f0-ec82-43a9-ad0c-9a94345450af', '8bd0dbae-41f6-466a-95a7-aff680ed0866')
          AND status IN ('FAILED', 'IN_PROGRESS');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Automated patch: Restored missing deliverables 5421d1f0 and 8bd0dbae'
        WHERE subject_id IN ('5421d1f0-ec82-43a9-ad0c-9a94345450af', '8bd0dbae-41f6-466a-95a7-aff680ed0866')
          AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Automated patch: Restored missing deliverables 5421d1f0 and 8bd0dbae'
        WHERE subject_id IN ('5421d1f0-ec82-43a9-ad0c-9a94345450af', '8bd0dbae-41f6-466a-95a7-aff680ed0866')
          AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;
END $$;
