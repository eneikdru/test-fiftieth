-- Flyway Migration V20260823112541517: Unblock stuck subjects in operations auditor
-- Mandatory version: V20260823112541517

-- 1. Conditionally update orchestrator tables if present
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='tasks') THEN
        UPDATE tasks
        SET status = 'RESOLVED'
        WHERE id IN (
            '86bfe9d0-6033-446b-adda-6e70b27f3f51',
            'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
            'c4904e50-85af-4e98-8cef-f6cf92d74c30'
        ) AND status IN ('STUCK', 'PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='wishlist') THEN
        UPDATE wishlist
        SET status = 'RESOLVED'
        WHERE id IN (
            '86bfe9d0-6033-446b-adda-6e70b27f3f51',
            'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
            'c4904e50-85af-4e98-8cef-f6cf92d74c30'
        ) AND status IN ('STUCK', 'PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;
END $$;

-- 2. Atomically-guarded updates on application privacy tables
UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Unblocked stuck subject in operations auditor'
WHERE subject_id IN (
    '86bfe9d0-6033-446b-adda-6e70b27f3f51',
    'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
    'c4904e50-85af-4e98-8cef-f6cf92d74c30'
)
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Unblocked stuck subject in operations auditor'
WHERE subject_id IN (
    '86bfe9d0-6033-446b-adda-6e70b27f3f51',
    'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
    'c4904e50-85af-4e98-8cef-f6cf92d74c30'
)
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
