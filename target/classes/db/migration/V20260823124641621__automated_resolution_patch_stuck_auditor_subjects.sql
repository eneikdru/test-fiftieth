-- Flyway Migration V20260823124641621: Automated resolution patch for stuck auditor subjects
-- Mandatory version: V20260823124641621

-- Subject 86bfe9d0-6033-446b-adda-6e70b27f3f51
UPDATE privacy_export_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    notes = 'Flagged for human review: Blocked task 96a47cb5 retired by iteration-admission poka-yoke; no child work created. Human judgment needed.'
WHERE subject_id = '86bfe9d0-6033-446b-adda-6e70b27f3f51'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    reason = 'Flagged for human review: Blocked task 96a47cb5 retired by iteration-admission poka-yoke; no child work created. Human judgment needed.'
WHERE subject_id = '86bfe9d0-6033-446b-adda-6e70b27f3f51'
  AND status IN ('PENDING', 'PROCESSING');


-- Subject ae8e1efb-88e8-4a17-83fb-942e06d65d53
UPDATE privacy_export_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    notes = 'Flagged for human review: Derived task a567a371 failed because PR#64 was closed without merge. Human judgment needed.'
WHERE subject_id = 'ae8e1efb-88e8-4a17-83fb-942e06d65d53'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    reason = 'Flagged for human review: Derived task a567a371 failed because PR#64 was closed without merge. Human judgment needed.'
WHERE subject_id = 'ae8e1efb-88e8-4a17-83fb-942e06d65d53'
  AND status IN ('PENDING', 'PROCESSING');


-- Subject c4904e50-85af-4e98-8cef-f6cf92d74c30
UPDATE privacy_export_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    notes = 'Flagged for human review: Derived task 6de4f222 failed with mechanical retirement. Human judgment needed.'
WHERE subject_id = 'c4904e50-85af-4e98-8cef-f6cf92d74c30'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    reason = 'Flagged for human review: Derived task 6de4f222 failed with mechanical retirement. Human judgment needed.'
WHERE subject_id = 'c4904e50-85af-4e98-8cef-f6cf92d74c30'
  AND status IN ('PENDING', 'PROCESSING');


-- Subject f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b
UPDATE privacy_export_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    notes = 'Flagged for human review: Task 49bf6c01 is an orphaned_dependency_chain. Human judgment needed.'
WHERE subject_id = 'f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    reason = 'Flagged for human review: Task 49bf6c01 is an orphaned_dependency_chain. Human judgment needed.'
WHERE subject_id = 'f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b'
  AND status IN ('PENDING', 'PROCESSING');


-- Subject 6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625
UPDATE privacy_export_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    notes = 'Flagged for human review: Task fbf3ff02 is an orphaned_dependency_chain. Escalating for a human to reconcile the two.'
WHERE subject_id = '6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'FLAGGED_FOR_HUMAN_REVIEW',
    reason = 'Flagged for human review: Task fbf3ff02 is an orphaned_dependency_chain. Escalating for a human to reconcile the two.'
WHERE subject_id = '6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625'
  AND status IN ('PENDING', 'PROCESSING');
