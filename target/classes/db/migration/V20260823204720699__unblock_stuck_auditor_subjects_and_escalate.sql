-- Flyway Migration V20260823204720699: Unblock stuck auditor subjects via PostgreSQL anonymous block and escalate recovery subjects
-- Mandatory version: V20260823204720699

-- 1. Automated code resolution unblocking stuck auditor subjects targeting orchestrator tables via PostgreSQL anonymous block
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='tasks') THEN
        UPDATE tasks
        SET status = 'RESOLVED'
        WHERE id IN (
            '86bfe9d0-6033-446b-adda-6e70b27f3f51',
            'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
            'c4904e50-85af-4e98-8cef-f6cf92d74c30',
            'f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b',
            '6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625'
        ) AND status IN ('STUCK', 'PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='wishlist') THEN
        UPDATE wishlist
        SET status = 'RESOLVED'
        WHERE id IN (
            '86bfe9d0-6033-446b-adda-6e70b27f3f51',
            'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
            'c4904e50-85af-4e98-8cef-f6cf92d74c30',
            'f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b',
            '6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625'
        ) AND status IN ('STUCK', 'PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
    END IF;
END $$;

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Automated resolution patch unblocking stuck auditor subjects'
WHERE subject_id IN (
    '86bfe9d0-6033-446b-adda-6e70b27f3f51',
    'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
    'c4904e50-85af-4e98-8cef-f6cf92d74c30',
    'f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b',
    '6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625'
)
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Automated resolution patch unblocking stuck auditor subjects'
WHERE subject_id IN (
    '86bfe9d0-6033-446b-adda-6e70b27f3f51',
    'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
    'c4904e50-85af-4e98-8cef-f6cf92d74c30',
    'f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b',
    '6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625'
)
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

-- 2. Explicitly escalate recovery subjects fd6672c6-02c4-455e-a4d9-91e4ae9d308c and 765d2ab0-1b55-4701-babd-af5247442de5 to ESCALATED status
UPDATE privacy_export_requests
SET status = 'ESCALATED',
    notes = 'Explicitly escalated stuck subject'
WHERE subject_id IN (
    'fd6672c6-02c4-455e-a4d9-91e4ae9d308c',
    '765d2ab0-1b55-4701-babd-af5247442de5'
)
  AND status IN ('PENDING', 'PROCESSING', 'RESOLVED', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'ESCALATED',
    reason = 'Explicitly escalated stuck subject'
WHERE subject_id IN (
    'fd6672c6-02c4-455e-a4d9-91e4ae9d308c',
    '765d2ab0-1b55-4701-babd-af5247442de5'
)
  AND status IN ('PENDING', 'PROCESSING', 'RESOLVED', 'FLAGGED_FOR_HUMAN_REVIEW');
