-- Implementation of the missing datastore contract 9b58412d
-- Explicit migration number from the prompt: V20260823124635268

CREATE INDEX IF NOT EXISTS idx_documents_publication_year ON documents(publication_year);
CREATE INDEX IF NOT EXISTS idx_documents_author_organization ON documents(author_organization);
