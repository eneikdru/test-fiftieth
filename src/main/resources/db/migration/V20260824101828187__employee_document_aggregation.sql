CREATE TABLE IF NOT EXISTS employee_documents (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(100) NOT NULL,
    doc_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    doc_date DATE NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_employee_docs_employee_id ON employee_documents(employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_docs_type ON employee_documents(doc_type);

INSERT INTO employee_documents (employee_id, doc_type, title, doc_date, details, created_at) VALUES
('EMP-001', 'ORDER', 'Приказ о назначении старшим научным сотрудником', '2023-01-15', 'Приказ №42-к о кадровом перемещении', CURRENT_TIMESTAMP),
('EMP-001', 'REPORT', 'Отчет по исследованию распространения гриппа', '2023-06-20', 'Годовой отчет лаборатории эпидемиологии', CURRENT_TIMESTAMP),
('EMP-001', 'EXAM', 'Протокол квалификационного экзамена по эпидемиологии', '2023-11-10', 'Оценка: отлично', CURRENT_TIMESTAMP),
('EMP-001', 'EXTRACT', 'Выписка из решения ученого совета', '2024-02-05', 'Протокол №5 заседания ученого совета', CURRENT_TIMESTAMP),
('EMP-002', 'ORDER', 'Приказ о зачислении в аспирантуру', '2022-09-01', 'Приказ №12-а', CURRENT_TIMESTAMP);
