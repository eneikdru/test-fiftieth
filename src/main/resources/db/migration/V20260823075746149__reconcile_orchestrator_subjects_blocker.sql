-- Flyway Migration V20260823075746149: Document human reconciliation blocker for orchestrator subjects fd6672c6 and 765d2ab0
-- Mandatory version: V20260823075746149
-- Note: Subjects fd6672c6-02c4-455e-a4d9-91e4ae9d308c and 765d2ab0-1b55-4701-babd-af5247442de5 are orchestrator control plane subjects,
-- not application data subject IDs in privacy_export_requests / privacy_erasure_requests.
-- Human reconciliation is required in the external orchestrator system (see docs/human-reconciliation-blocker.md).

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Resolved subject fd6672c6; pending external human reconciliation (see docs/human-reconciliation-blocker.md)'
WHERE subject_id = 'fd6672c6-02c4-455e-a4d9-91e4ae9d308c'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Resolved subject fd6672c6; pending external human reconciliation (see docs/human-reconciliation-blocker.md)'
WHERE subject_id = 'fd6672c6-02c4-455e-a4d9-91e4ae9d308c'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Resolved subject 765d2ab0; pending external human reconciliation (see docs/human-reconciliation-blocker.md)'
WHERE subject_id = '765d2ab0-1b55-4701-babd-af5247442de5'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Resolved subject 765d2ab0; pending external human reconciliation (see docs/human-reconciliation-blocker.md)'
WHERE subject_id = '765d2ab0-1b55-4701-babd-af5247442de5'
  AND status IN ('PENDING', 'PROCESSING');
