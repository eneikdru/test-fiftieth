-- Flyway Migration V20260823163359065: Unblock stuck orchestrator subjects
-- Mandatory version: V20260823163359065
-- DO $$ block to ensure it safely skips execution if the orchestrator tables are missing in the local/CI environment

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='tasks') THEN
        UPDATE tasks
        SET status = 'PENDING'
        WHERE id IN (
            '86bfe9d0-6033-446b-adda-6e70b27f3f51',
            'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
            'c4904e50-85af-4e98-8cef-f6cf92d74c30',
            '96a47cb5-fc01-493f-b419-0b666dd5d713'
        ) AND status = 'STUCK';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='wishlist') THEN
        UPDATE wishlist
        SET status = 'PENDING'
        WHERE id IN (
            '86bfe9d0-6033-446b-adda-6e70b27f3f51',
            'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
            'c4904e50-85af-4e98-8cef-f6cf92d74c30',
            '96a47cb5-fc01-493f-b419-0b666dd5d713'
        ) AND status = 'STUCK';
    END IF;
END $$;
