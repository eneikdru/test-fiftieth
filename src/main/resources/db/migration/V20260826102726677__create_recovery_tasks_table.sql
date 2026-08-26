-- Flyway Migration V20260826102726677: Create recovery_tasks table for resuming retired plan tasks
-- Domain: Task Recovery & Datastore Alignment

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
