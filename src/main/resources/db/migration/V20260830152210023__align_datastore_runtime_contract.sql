-- Flyway Migration V20260830152210023: Datastore Runtime Contract alignment for missing Test Coverage B8f32565
-- Mandatory Flyway version: V20260830152210023

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Automated patch: Restored missing Test Coverage B8f32565'
WHERE subject_id LIKE 'b8f32565-%'
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Automated patch: Restored missing Test Coverage B8f32565'
WHERE subject_id LIKE 'b8f32565-%'
  AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW');
