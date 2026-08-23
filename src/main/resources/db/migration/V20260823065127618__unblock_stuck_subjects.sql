-- Unblock stuck subjects fd6672c6-02c4-455e-a4d9-91e4ae9d308c and 765d2ab0-1b55-4701-babd-af5247442de5

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Resolved stuck subject from iteration-admission poka-yoke failure in task ea2d1954-4da4-4912-8565-dcd27a569279'
WHERE subject_id = 'fd6672c6-02c4-455e-a4d9-91e4ae9d308c'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Resolved stuck subject from iteration-admission poka-yoke failure in task ea2d1954-4da4-4912-8565-dcd27a569279'
WHERE subject_id = 'fd6672c6-02c4-455e-a4d9-91e4ae9d308c'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Resolved stuck subject with orphaned dependency 168a6edf-53fa-48b4-8226-9fcd0528efd2'
WHERE subject_id = '765d2ab0-1b55-4701-babd-af5247442de5'
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Resolved stuck subject with orphaned dependency 168a6edf-53fa-48b4-8226-9fcd0528efd2'
WHERE subject_id = '765d2ab0-1b55-4701-babd-af5247442de5'
  AND status IN ('PENDING', 'PROCESSING');
