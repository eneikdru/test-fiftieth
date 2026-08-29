-- Seed initial database content with isolated strains and dossier records
INSERT INTO employee_documents (employee_id, employee_surname, doc_type, title, doc_date, details, scientific_direction, created_at) VALUES
('EMP-007', 'Иванов', 'STRAIN_ISOLATION', 'Изоляция штамма SARS-CoV-2 (штамм B.1.1.529/Омикрон)', '2024-03-15', 'Геномное секвенирование выделенного изолята показало высокую контагиозность. Образец сохранен в криобанке.', 'Вирусология', CURRENT_TIMESTAMP),
('EMP-007', 'Иванов', 'STRAIN_ISOLATION', 'Штамм гриппа A/H3N2 (Сезонный высев 2024)', '2024-04-10', 'Выделен антигенный вариант штамма из клиник эндемичного региона.', 'Эпиднадзор', CURRENT_TIMESTAMP),
('EMP-008', 'Петрова', 'DOSSIER_ENTRY', 'Карточка эпидемиологического досье очага №408', '2024-05-02', 'Сводное досье расследования эпидемического очага с характеристикой выделенных штаммов.', 'Эпидемиология', CURRENT_TIMESTAMP);

INSERT INTO dossier_reports (employee_id, template_type, status, summary_text, document_count, download_url, created_at) VALUES
('EMP-007', 'SUMMARY_STANDARD', 'COMPLETED', 'Итоговый отчет по выделенным штаммам вирусов за первый квартал 2024 года. Выделено и охарактеризовано 2 штамма.', 2, '/api/v1/dossier/reports/101/download', CURRENT_TIMESTAMP),
('EMP-008', 'FULL_DOSSIER', 'COMPLETED', 'Полное досье эпидемиологического расследования по очагу №408.', 1, '/api/v1/dossier/reports/102/download', CURRENT_TIMESTAMP);
