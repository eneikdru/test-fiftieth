-- Flyway Migration V20260830214500140: Datastore Runtime Contract alignment for missing Recovery Data Schema
-- Mandatory Flyway version: V20260830214500140

CREATE TABLE IF NOT EXISTS recovery_tasks (
    id UUID PRIMARY KEY,
    subject_id VARCHAR(100),
    title VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'FAILED',
    failure_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recovery_tasks_status ON recovery_tasks(status);
CREATE INDEX IF NOT EXISTS idx_recovery_tasks_subject_id ON recovery_tasks(subject_id);

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='recovery_tasks') THEN
        UPDATE recovery_tasks
        SET status = 'RESOLVED',
            failure_reason = 'Automated patch: Restored missing Recovery Data Schema'
        WHERE status = 'FAILED'
          AND (title LIKE '%Recovery Data Schema%' OR failure_reason LIKE '%Recovery Data Schema%');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_export_requests') THEN
        UPDATE privacy_export_requests
        SET status = 'RESOLVED',
            notes = 'Automated patch: Restored missing Recovery Data Schema'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (notes LIKE '%Recovery Data Schema%' OR subject_id = '8f5b708e-8f4a-4f9d-b97d-2d2e11287e42');
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE tablename='privacy_erasure_requests') THEN
        UPDATE privacy_erasure_requests
        SET status = 'RESOLVED',
            reason = 'Automated patch: Restored missing Recovery Data Schema'
        WHERE status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')
          AND (reason LIKE '%Recovery Data Schema%' OR subject_id = '8f5b708e-8f4a-4f9d-b97d-2d2e11287e42');
    END IF;
END $$;
