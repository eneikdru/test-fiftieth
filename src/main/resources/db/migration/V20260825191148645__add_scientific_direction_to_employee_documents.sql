ALTER TABLE employee_documents ADD COLUMN IF NOT EXISTS scientific_direction VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_employee_docs_direction ON employee_documents(scientific_direction);

INSERT INTO employee_documents (employee_id, doc_type, title, doc_date, details, created_at, scientific_direction) VALUES
('EMP-003', 'PUBLICATION', 'Анализ вспышек кори', '2023-04-12', 'Статья в журнале', CURRENT_TIMESTAMP, 'Эпиднадзор'),
('EMP-004', 'REPORT', 'Отчет по разработке вакцины', '2023-08-25', 'Годовой отчет', CURRENT_TIMESTAMP, 'Вакцинопрофилактика'),
('EMP-005', 'PUBLICATION', 'Новые штаммы гриппа', '2023-11-05', 'Исследование мутаций', CURRENT_TIMESTAMP, 'Вирусология'),
('EMP-006', 'ORDER', 'Приказ о начале клинических испытаний', '2024-01-20', 'Приказ №55', CURRENT_TIMESTAMP, 'Бактериология');
