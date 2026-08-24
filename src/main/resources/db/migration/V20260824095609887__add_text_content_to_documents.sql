ALTER TABLE documents ADD COLUMN text_content TEXT;
CREATE INDEX idx_documents_text_content ON documents USING GIN (to_tsvector('russian', text_content));
