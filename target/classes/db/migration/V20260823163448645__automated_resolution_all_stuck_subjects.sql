-- Flyway Migration V20260823163448645: Automated patch to unblock stuck operations auditor subjects
-- Mandatory version: V20260823163448645

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Automated patch: Resolved stuck subject without human intervention due to missing human in loop.'
WHERE subject_id IN (
    '86bfe9d0-6033-446b-adda-6e70b27f3f51',
    'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
    'c4904e50-85af-4e98-8cef-f6cf92d74c30',
    '6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625',
    'f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b',
    '30b258b2-0f02-4605-8d7a-1ecb1b4bebbb'
)
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Automated patch: Resolved stuck subject without human intervention due to missing human in loop.'
WHERE subject_id IN (
    '86bfe9d0-6033-446b-adda-6e70b27f3f51',
    'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
    'c4904e50-85af-4e98-8cef-f6cf92d74c30',
    '6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625',
    'f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b',
    '30b258b2-0f02-4605-8d7a-1ecb1b4bebbb'
)
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
