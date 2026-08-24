-- Flyway Migration V20260824004538228: Datastore Runtime Contract 9b58412d patch alignment
-- Mandatory Flyway version: V20260824004538228

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='orchestrator_tasks') THEN
        UPDATE orchestrator_tasks
        SET status = 'RESOLVED'
        WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='orchestrator_wishlist') THEN
        UPDATE orchestrator_wishlist
        SET status = 'RESOLVED'
        WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866';
    END IF;
END $$;
