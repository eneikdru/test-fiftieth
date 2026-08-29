-- Flyway Migration V20260823170105641: Resolve out-of-control review concerns pattern
-- Mandatory version: V20260823170105641

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename='design_review_concerns') THEN
        INSERT INTO design_review_concerns (id, stream_name, epic_sequence, u_value, root_cause_pattern_id, status, created_at)
        VALUES ('CONCERN-EPIC-10', 'reviewConcerns', 10, 0.0000, 'RCP-REVIEW-CONCERNS-001', 'CATEGORIZED', CURRENT_TIMESTAMP)
        ON CONFLICT (id) DO UPDATE
        SET root_cause_pattern_id = 'RCP-REVIEW-CONCERNS-001',
            status = 'CATEGORIZED';
    END IF;
END $$;
