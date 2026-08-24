-- Unblock stuck subjects fd6672c6-02c4-455e-a4d9-91e4ae9d308c and 765d2ab0-1b55-4701-babd-af5247442de5

UPDATE privacy_export_requests
SET status = 'RESOLVED',
    notes = 'Unblocked stuck subject fd6672c6 or 765d2ab0'
WHERE subject_id IN ('fd6672c6-02c4-455e-a4d9-91e4ae9d308c', '765d2ab0-1b55-4701-babd-af5247442de5')
  AND status IN ('PENDING', 'PROCESSING');

UPDATE privacy_erasure_requests
SET status = 'RESOLVED',
    reason = 'Unblocked stuck subject fd6672c6 or 765d2ab0'
WHERE subject_id IN ('fd6672c6-02c4-455e-a4d9-91e4ae9d308c', '765d2ab0-1b55-4701-babd-af5247442de5')
  AND status IN ('PENDING', 'PROCESSING');
