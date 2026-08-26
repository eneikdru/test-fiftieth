-- Flyway Migration V20260825170451994: Revive stuck task 8bd0dbae-41f6-466a-95a7-aff680ed0866
-- Mandatory Flyway version: V20260825170451994

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='orchestrator_tasks') THEN
        UPDATE orchestrator_tasks
        SET status = 'IN_PROGRESS'
        WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='orchestrator_wishlist') THEN
        UPDATE orchestrator_wishlist
        SET status = 'IN_PROGRESS'
        WHERE subject_id = '8bd0dbae-41f6-466a-95a7-aff680ed0866';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='tasks') THEN
        UPDATE tasks
        SET status = 'IN_PROGRESS'
        WHERE id = '8bd0dbae-41f6-466a-95a7-aff680ed0866';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='wishlist') THEN
        UPDATE wishlist
        SET status = 'IN_PROGRESS'
        WHERE id = '8bd0dbae-41f6-466a-95a7-aff680ed0866';
    END IF;
END $$;
