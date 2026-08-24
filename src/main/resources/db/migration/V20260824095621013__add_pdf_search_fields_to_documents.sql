ALTER TABLE documents ADD COLUMN doc_type VARCHAR(50);
ALTER TABLE documents ADD COLUMN publication_date DATE;

UPDATE documents SET doc_type = 'REPORT', publication_date = '2023-05-15', text_content = 'Протокол расследования вспышки сальмонеллеза. Ответственный исследователь Иванов И.И.' WHERE id = 1;
UPDATE documents SET doc_type = 'REPORT', publication_date = '2023-08-10', text_content = 'Отчет эпиднадзора по гриппу. Докладчик Петров П.П.' WHERE id = 2;
UPDATE documents SET doc_type = 'ORDER', publication_date = '2021-03-01', text_content = 'Методические рекомендации по профилактике кори. Утвердил Сидоров С.С.' WHERE id = 3;
