-- Flyway Migration V20260823102338637: Resolve stuck operations auditor subjects
-- Mandatory version: V20260823102338637
-- Target subjects: 86bfe9d0-6033-446b-adda-6e70b27f3f51, ae8e1efb-88e8-4a17-83fb-942e06d65d53, c4904e50-85af-4e98-8cef-f6cf92d74c30, 96a47cb5-fc01-493f-b419-0b666dd5d713

-- Subject 1: 86bfe9d0-6033-446b-adda-6e70b27f3f51
UPDATE privacy_export_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    notes = 'Flagged for human review: Wishlist 86bfe9d0 task 96a47cb5 terminally failed with poka-yoke retirement; human judgment required on requirement validity'
WHERE subject_id = '86bfe9d0-6033-446b-adda-6e70b27f3f51'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    reason = 'Flagged for human review: Wishlist 86bfe9d0 task 96a47cb5 terminally failed with poka-yoke retirement; human judgment required on requirement validity'
WHERE subject_id = '86bfe9d0-6033-446b-adda-6e70b27f3f51'
  AND status IN ('PENDING', 'PROCESSING');

-- Subject 2: ae8e1efb-88e8-4a17-83fb-942e06d65d53
UPDATE privacy_export_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    notes = 'Flagged for human review: Wishlist ae8e1efb task a567a371 failed due to closed PR#64 without merge; human confirmation needed on recovery vs dismissal path'
WHERE subject_id = 'ae8e1efb-88e8-4a17-83fb-942e06d65d53'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    reason = 'Flagged for human review: Wishlist ae8e1efb task a567a371 failed due to closed PR#64 without merge; human confirmation needed on recovery vs dismissal path'
WHERE subject_id = 'ae8e1efb-88e8-4a17-83fb-942e06d65d53'
  AND status IN ('PENDING', 'PROCESSING');

-- Subject 3: c4904e50-85af-4e98-8cef-f6cf92d74c30
UPDATE privacy_export_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    notes = 'Flagged for human review: Wishlist c4904e50 task 6de4f222 retired by poka-yoke; product judgment required to determine if scope is obsolete'
WHERE subject_id = 'c4904e50-85af-4e98-8cef-f6cf92d74c30'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    reason = 'Flagged for human review: Wishlist c4904e50 task 6de4f222 retired by poka-yoke; product judgment required to determine if scope is obsolete'
WHERE subject_id = 'c4904e50-85af-4e98-8cef-f6cf92d74c30'
  AND status IN ('PENDING', 'PROCESSING');

-- Subject 4: 96a47cb5-fc01-493f-b419-0b666dd5d713
UPDATE privacy_export_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    notes = 'Flagged for human review: Task 96a47cb5 retired by poka-yoke; human review required to confirm underlying blocker resolution'
WHERE subject_id = '96a47cb5-fc01-493f-b419-0b666dd5d713'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    reason = 'Flagged for human review: Task 96a47cb5 retired by poka-yoke; human review required to confirm underlying blocker resolution'
WHERE subject_id = '96a47cb5-fc01-493f-b419-0b666dd5d713'
  AND status IN ('PENDING', 'PROCESSING');
