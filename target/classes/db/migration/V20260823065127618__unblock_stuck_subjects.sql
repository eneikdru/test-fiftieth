-- Unblock stuck subjects fd6672c6-02c4-455e-a4d9-91e4ae9d308c and 765d2ab0-1b55-4701-babd-af5247442de5

DO $$
BEGIN
  IF EXISTS (SELECT FROM pg_tables WHERE tablename='orchestrator_tasks') THEN
    UPDATE orchestrator_tasks SET status = 'RESOLVED' WHERE subject_id = 'fd6672c6-02c4-455e-a4d9-91e4ae9d308c';
    UPDATE orchestrator_tasks SET status = 'RESOLVED' WHERE subject_id = '765d2ab0-1b55-4701-babd-af5247442de5';
  END IF;
  IF EXISTS (SELECT FROM pg_tables WHERE tablename='orchestrator_wishlist') THEN
    UPDATE orchestrator_wishlist SET status = 'RESOLVED' WHERE subject_id = 'fd6672c6-02c4-455e-a4d9-91e4ae9d308c';
    UPDATE orchestrator_wishlist SET status = 'RESOLVED' WHERE subject_id = '765d2ab0-1b55-4701-babd-af5247442de5';
  END IF;
END $$;
