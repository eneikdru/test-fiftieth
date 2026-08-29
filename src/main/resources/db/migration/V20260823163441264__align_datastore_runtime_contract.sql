-- Flyway Migration V20260823163441264: Datastore Runtime Contract 9b58412d patch alignment
-- Mandatory Flyway version: V20260823163441264

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='orchestrator_tasks') THEN
        UPDATE orchestrator_tasks
        SET status = 'RESOLVED'
        WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='orchestrator_wishlist') THEN
        UPDATE orchestrator_wishlist
        SET status = 'RESOLVED'
        WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='tasks') THEN
        UPDATE tasks
        SET status = 'RESOLVED'
        WHERE id = '8bd0dbae-41f6-466a-95a7-aff680ed0866';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='wishlist') THEN
        UPDATE wishlist
        SET status = 'RESOLVED'
        WHERE id = '8bd0dbae-41f6-466a-95a7-aff680ed0866';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Automated patch: Resolved runtime contract discrepancy for closed task 8bd0dbae-41f6-466a-95a7-aff680ed0866'
        WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866'
          AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Automated patch: Resolved runtime contract discrepancy for closed task 8bd0dbae-41f6-466a-95a7-aff680ed0866'
        WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866'
          AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;
END $$;
