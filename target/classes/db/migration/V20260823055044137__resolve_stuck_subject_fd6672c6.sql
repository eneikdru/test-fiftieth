-- Resolve stuck subject fd6672c6-02c4-455e-a4d9-91e4ae9d308c from task ea2d1954-4da4-4912-8565-dcd27a569279
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
