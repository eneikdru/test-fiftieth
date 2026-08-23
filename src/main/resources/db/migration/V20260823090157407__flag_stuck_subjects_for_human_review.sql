-- Flyway Migration V20260823090157407: Flag stuck subjects for human review
-- Mandatory version: V20260823090157407
-- Subject fd6672c6-02c4-455e-a4d9-91e4ae9d308c and subject 765d2ab0-1b55-4701-babd-af5247442de5

UPDATE privacy_export_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    notes = 'Flagged for human review: Blocked task ea2d1954-4da4-4912-8565-dcd27a569279 retired by poka-yoke with no child work created'
WHERE subject_id = 'fd6672c6-02c4-455e-a4d9-91e4ae9d308c'
  AND status IN ('PENDING', 'PROCESSING', 'RESOLVED');

UPDATE privacy_erasure_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    reason = 'Flagged for human review: Blocked task ea2d1954-4da4-4912-8565-dcd27a569279 retired by poka-yoke with no child work created'
WHERE subject_id = 'fd6672c6-02c4-455e-a4d9-91e4ae9d308c'
  AND status IN ('PENDING', 'PROCESSING', 'RESOLVED');

UPDATE privacy_export_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    notes = 'Flagged for human review: Blocked task 168a6edf-9643-4775-a637-7c1803624025 retired by poka-yoke with no child work created'
WHERE subject_id = '765d2ab0-1b55-4701-babd-af5247442de5'
  AND status IN ('PENDING', 'PROCESSING', 'RESOLVED');

UPDATE privacy_erasure_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    reason = 'Flagged for human review: Blocked task 168a6edf-9643-4775-a637-7c1803624025 retired by poka-yoke with no child work created'
WHERE subject_id = '765d2ab0-1b55-4701-babd-af5247442de5'
  AND status IN ('PENDING', 'PROCESSING', 'RESOLVED');
