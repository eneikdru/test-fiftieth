CREATE TABLE documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author_organization VARCHAR(255) NOT NULL,
    publication_year INT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_documents_title ON documents(title);
CREATE INDEX idx_documents_author ON documents(author_organization);
CREATE INDEX idx_documents_year ON documents(publication_year);

INSERT INTO documents (title, author_organization, publication_year, file_path) VALUES
('Протокол эпидемиологического расследования вспышки сальмонеллеза', 'НИИ Эпидемиологии', 2023, '/data/docs/2023/salmonella_outbreak.pdf'),
('Отчет эпиднадзора по гриппу и ОРВИ за сезон 2022-2023', 'Центр мониторинга инфекций', 2023, '/data/docs/2023/flu_surveillance.pdf'),
('Методические рекомендации по профилактике кори', 'Министерство здравоохранения РФ', 2021, '/data/docs/2021/measles_prevention.docx');
