-- Flyway Migration V20260823095428075: Unblock auditor subjects via database updates
-- Mandatory Flyway version: V20260823095428075

-- Subject ae8e1efb-88e8-4a17-83fb-942e06d65d53: Flag for human adjudication
UPDATE privacy_export_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    notes = 'Flagged for human adjudication: Wishlist derived task failed due to PR#64 closed without merge'
WHERE subject_id = 'ae8e1efb-88e8-4a17-83fb-942e06d65d53'
  AND status IN ('PENDING', 'PROCESSING', 'STUCK', 'RESOLVED');

UPDATE privacy_erasure_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    reason = 'Flagged for human adjudication: Wishlist derived task failed due to PR#64 closed without merge'
WHERE subject_id = 'ae8e1efb-88e8-4a17-83fb-942e06d65d53'
  AND status IN ('PENDING', 'PROCESSING', 'STUCK', 'RESOLVED');

-- Subject a567a371-3dc4-490a-9a4b-bed7e9348f16: Flag for human adjudication
UPDATE privacy_export_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    notes = 'Flagged for human adjudication: Terminally failed task scope undelivered; PR#64 closed without merge'
WHERE subject_id = 'a567a371-3dc4-490a-9a4b-bed7e9348f16'
  AND status IN ('PENDING', 'PROCESSING', 'STUCK', 'RESOLVED');

UPDATE privacy_erasure_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    reason = 'Flagged for human adjudication: Terminally failed task scope undelivered; PR#64 closed without merge'
WHERE subject_id = 'a567a371-3dc4-490a-9a4b-bed7e9348f16'
  AND status IN ('PENDING', 'PROCESSING', 'STUCK', 'RESOLVED');

-- Subject c4904e50-85af-4e98-8cef-f6cf92d74c30: Keep active pending recovery task
UPDATE privacy_export_requests
SET status = 'PROCESSING',
    notes = 'Kept active pending recovery task outcome'
WHERE subject_id = 'c4904e50-85af-4e98-8cef-f6cf92d74c30'
  AND status IN ('PENDING', 'STUCK', 'FLAGGED_FOR_HUMAN_REVIEW', 'RESOLVED');

UPDATE privacy_erasure_requests
SET status = 'PROCESSING',
    reason = 'Kept active pending recovery task outcome'
WHERE subject_id = 'c4904e50-85af-4e98-8cef-f6cf92d74c30'
  AND status IN ('PENDING', 'STUCK', 'FLAGGED_FOR_HUMAN_REVIEW', 'RESOLVED');
