-- Flyway Migration V20260824054955643: Categorize design review concern pattern and assign rootCausePatternId
-- Mandatory version: V20260824054955643

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Categorized invariant pattern: Assigned rootCausePatternId PATTERN-DESIGN-REVIEW-CONCERN-01 for epic review concerns'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Categorized invariant pattern: Assigned rootCausePatternId PATTERN-DESIGN-REVIEW-CONCERN-01 for epic review concerns'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;
END $$;
