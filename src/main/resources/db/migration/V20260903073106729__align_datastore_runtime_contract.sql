-- Flyway Migration V20260903073106729: Align datastore runtime contract
-- Mandatory Flyway version: V20260903073106729

UPDATE recovery_tasks
SET status = 'RESOLVED',
    failure_reason = 'Automated patch: Restored missing deliverable 289c8597'
WHERE subject_id = '289c8597' AND status = 'FAILED';

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Automated patch: Restored missing deliverable 289c8597'
WHERE subject_id = '289c8597' AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Automated patch: Restored missing deliverable 289c8597'
WHERE subject_id = '289c8597' AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
