-- Flyway migration V20260917060053445: Add GIN full-text search index for documents table
CREATE INDEX IF NOT EXISTS idx_documents_fulltext ON documents USING gin(
    to_tsvector('russian', coalesce(title, '') || ' ' || coalesce(author_organization, '') || ' ' || coalesce(text_content, ''))
);
