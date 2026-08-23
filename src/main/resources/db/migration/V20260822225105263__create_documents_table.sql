CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    author_organization VARCHAR(255) NOT NULL,
    publication_year INTEGER NOT NULL,
    file_path VARCHAR(512) NOT NULL
);

INSERT INTO documents (title, author_organization, publication_year, file_path)
VALUES
    ('Протокол расследования вспышки сальмонеллеза', 'НИИ Эпидемиологии', 2023, '/data/protocols/salmonella_2023.pdf'),
    ('Отчет эпиднадзора по гриппу за 1 квартал', 'Роспотребнадзор', 2024, '/data/reports/flu_q1_2024.docx'),
    ('Методическое руководство по профилактике кори', 'НИИ Эпидемиологии', 2022, '/data/guidelines/measles_prevention.pdf');
