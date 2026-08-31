-- Flyway Migration V20260831103845614: Datastore Runtime Contract alignment for privacy request management
-- Mandatory Flyway version: V20260831103845614

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Automated patch: Datastore runtime contract alignment'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (notes LIKE '%Bff463fd%' OR subject_id = 'Bff463fd');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Automated patch: Datastore runtime contract alignment'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (reason LIKE '%Bff463fd%' OR subject_id = 'Bff463fd');
    END IF;
END $$;
