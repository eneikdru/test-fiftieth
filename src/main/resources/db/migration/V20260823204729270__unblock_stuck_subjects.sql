-- Flyway Migration V20260823204729270: Unblock stuck orchestrator subjects
-- Mandatory version: V20260823204729270
-- DO $$ block to ensure it safely skips execution if the orchestrator tables are missing in the local/CI environment

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='tasks') THEN
        UPDATE tasks
        SET status = 'ESCALATED'
        WHERE id IN (
            '86bfe9d0-6033-446b-adda-6e70b27f3f51',
            'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
            'c4904e50-85af-4e98-8cef-f6cf92d74c30',
            '6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625',
            'f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b',
            '30b258b2-0f02-4605-8d7a-1ecb1b4bebbb',
            '88d40685-9ffd-4318-98d9-14a2bd5adab8',
            '39f37999-e800-4993-a7bd-106e1811d212'
        );
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='wishlist') THEN
        UPDATE wishlist
        SET status = 'ESCALATED'
        WHERE id IN (
            '86bfe9d0-6033-446b-adda-6e70b27f3f51',
            'ae8e1efb-88e8-4a17-83fb-942e06d65d53',
            'c4904e50-85af-4e98-8cef-f6cf92d74c30',
            '6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625',
            'f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b',
            '30b258b2-0f02-4605-8d7a-1ecb1b4bebbb',
            '88d40685-9ffd-4318-98d9-14a2bd5adab8',
            '39f37999-e800-4993-a7bd-106e1811d212'
        );
    END IF;
END $$;
