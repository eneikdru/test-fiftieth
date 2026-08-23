-- Escalate stuck wishlist subjects fd6672c6-02c4-455e-a4d9-91e4ae9d308c and 765d2ab0-1b55-4701-babd-af5247442de5 for human judgment

UPDATE privacy_export_requests
SET status = 'ESCALATED',
    notes = 'Escalated stuck subject fd6672c6 for human judgment following iteration-admission poka-yoke retirement in task ea2d1954-4da4-4912-8565-dcd27a569279'
WHERE subject_id = 'fd6672c6-02c4-455e-a4d9-91e4ae9d308c'
  AND status IN ('PENDING', 'PROCESSING', 'RESOLVED');

UPDATE privacy_erasure_requests
SET status = 'ESCALATED',
    reason = 'Escalated stuck subject fd6672c6 for human judgment following iteration-admission poka-yoke retirement in task ea2d1954-4da4-4912-8565-dcd27a569279'
WHERE subject_id = 'fd6672c6-02c4-455e-a4d9-91e4ae9d308c'
  AND status IN ('PENDING', 'PROCESSING', 'RESOLVED');

UPDATE privacy_export_requests
SET status = 'ESCALATED',
    notes = 'Escalated stuck subject 765d2ab0 for human judgment following iteration-admission poka-yoke retirement in task 168a6edf-9643-4775-a637-7c1803624025'
WHERE subject_id = '765d2ab0-1b55-4701-babd-af5247442de5'
  AND status IN ('PENDING', 'PROCESSING', 'RESOLVED');

UPDATE privacy_erasure_requests
SET status = 'ESCALATED',
    reason = 'Escalated stuck subject 765d2ab0 for human judgment following iteration-admission poka-yoke retirement in task 168a6edf-9643-4775-a637-7c1803624025'
WHERE subject_id = '765d2ab0-1b55-4701-babd-af5247442de5'
  AND status IN ('PENDING', 'PROCESSING', 'RESOLVED');
