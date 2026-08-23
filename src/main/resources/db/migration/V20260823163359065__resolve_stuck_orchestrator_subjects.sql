-- Mandatory version: V20260823163359065
-- Move subjects out of stuck state in orchestrator task/wishlist tables
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'tasks') THEN
        UPDATE tasks SET status = 'RESOLVED' WHERE subject_id IN ('86bfe9d0-6033-446b-adda-6e70b27f3f51', 'ae8e1efb-88e8-4a17-83fb-942e06d65d53', 'c4904e50-85af-4e98-8cef-f6cf92d74c30', '96a47cb5-fc01-493f-b419-0b666dd5d713') AND status IN ('PENDING', 'PROCESSING', 'STUCK');
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'wishlist') THEN
        UPDATE wishlist SET status = 'RESOLVED' WHERE subject_id IN ('86bfe9d0-6033-446b-adda-6e70b27f3f51', 'ae8e1efb-88e8-4a17-83fb-942e06d65d53', 'c4904e50-85af-4e98-8cef-f6cf92d74c30', '96a47cb5-fc01-493f-b419-0b666dd5d713') AND status IN ('PENDING', 'PROCESSING', 'STUCK');
    END IF;
END $$;
