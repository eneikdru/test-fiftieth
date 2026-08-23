-- Ручной disaster-recovery путь для отката миграции V20260822225105263
-- Выполнить вручную в случае необходимости удаления таблицы documents

DROP INDEX IF EXISTS idx_documents_year;
DROP INDEX IF EXISTS idx_documents_author;
DROP INDEX IF EXISTS idx_documents_title;

DROP TABLE IF EXISTS documents;
