CREATE TABLE IF NOT EXISTS organizations (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS document_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS hazard_categories (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS dossier_report_documents (
    id BIGSERIAL PRIMARY KEY,
    dossier_report_id BIGINT NOT NULL REFERENCES dossier_reports(id) ON DELETE CASCADE,
    employee_document_id BIGINT NOT NULL REFERENCES employee_documents(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_dossier_report_employee_doc UNIQUE (dossier_report_id, employee_document_id)
);

CREATE INDEX IF NOT EXISTS idx_dossier_report_docs_report_id ON dossier_report_documents(dossier_report_id);
CREATE INDEX IF NOT EXISTS idx_dossier_report_docs_doc_id ON dossier_report_documents(employee_document_id);

INSERT INTO organizations (code, name, description) VALUES
('NII_EPI', 'НИИ Эпидемиологии', 'Научно-исследовательский институт эпидемиологии'),
('CENTER_INF', 'Центр мониторинга инфекций', 'Центр гигиены и эпидемиологии мониторинга инфекционных болезней'),
('MINZDRAV', 'Министерство здравоохранения РФ', 'Федеральный орган исполнительной власти в сфере здравоохранения')
ON CONFLICT (code) DO NOTHING;

INSERT INTO document_types (code, name, description) VALUES
('ORDER', 'Приказ', 'Кадровый или административный приказ'),
('REPORT', 'Отчет', 'Эпидемиологический или аналитический отчет'),
('EXAM', 'Экзамен', 'Протокол квалификационного экзамена'),
('EXTRACT', 'Выписка', 'Выписка из решения ученого совета или заседания'),
('DOSSIER_ENTRY', 'Запись досье', 'Запись эпидемиологического досье очага')
ON CONFLICT (code) DO NOTHING;

INSERT INTO hazard_categories (code, name, description) VALUES
('RESPIRATORY', 'Респираторный очаг', 'Очаги инфекций с воздушно-капельным механизмом передачи'),
('FOODBORNE', 'Пищевой очаг', 'Очаги инфекций с фекально-оральным (пищевым) механизмом передачи'),
('ZOONOTIC', 'Зоонозный очаг', 'Очаги инфекций, передающихся от животных к человеку'),
('BLOODBORNE', 'Кровяной очаг', 'Очаги инфекций с гемоконтактным механизмом передачи')
ON CONFLICT (code) DO NOTHING;
