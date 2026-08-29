-- Flyway Migration V20260823081040819: Escalate stuck subjects
-- Mandatory version: V20260823081040819

UPDATE privacy_export_requests
SET status = 'ESCALATED',
    notes = 'Escalated stuck subject from iteration-admission poka-yoke failure'
WHERE subject_id = 'fd6672c6-02c4-455e-a4d9-91e4ae9d308c'
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'ESCALATED',
    reason = 'Escalated stuck subject from iteration-admission poka-yoke failure'
WHERE subject_id = 'fd6672c6-02c4-455e-a4d9-91e4ae9d308c'
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_export_requests
SET status = 'ESCALATED',
    notes = 'Escalated stuck subject with orphaned dependency'
WHERE subject_id = '765d2ab0-1b55-4701-babd-af5247442de5'
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'ESCALATED',
    reason = 'Escalated stuck subject with orphaned dependency'
WHERE subject_id = '765d2ab0-1b55-4701-babd-af5247442de5'
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
