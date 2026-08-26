-- Flyway Migration V20260825230033325: Revive stuck task 5421d1f0-ec82-43a9-ad0c-9a94345450af
-- Mandatory Flyway version: V20260825230033325

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='orchestrator_tasks') THEN
        UPDATE orchestrator_tasks
        SET status = 'IN_PROGRESS'
        WHERE subject_id = '5421d1f0-ec82-43a9-ad0c-9a94345450af';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='orchestrator_wishlist') THEN
        UPDATE orchestrator_wishlist
        SET status = 'IN_PROGRESS'
        WHERE subject_id = '5421d1f0-ec82-43a9-ad0c-9a94345450af';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='tasks') THEN
        UPDATE tasks
        SET status = 'IN_PROGRESS'
        WHERE id = '5421d1f0-ec82-43a9-ad0c-9a94345450af';
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='wishlist') THEN
        UPDATE wishlist
        SET status = 'IN_PROGRESS'
        WHERE id = '5421d1f0-ec82-43a9-ad0c-9a94345450af';
    END IF;
END $$;
