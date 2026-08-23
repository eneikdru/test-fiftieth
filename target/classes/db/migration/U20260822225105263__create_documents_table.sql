-- Rollback for V20260822225105263__create_documents_table
-- Note: This is a manual disaster recovery instruction, as Flyway Community does not support auto-undo.

DROP TABLE IF EXISTS documents;
