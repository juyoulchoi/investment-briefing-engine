ALTER TABLE tb_investment_briefing
    ADD COLUMN title VARCHAR(200),
    ADD COLUMN briefing_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

UPDATE tb_investment_briefing
SET title = COALESCE(NULLIF(headline, ''), '투자 브리핑 ' || TO_CHAR(briefing_date, 'YYYY-MM-DD'));

ALTER TABLE tb_investment_briefing
    ALTER COLUMN title SET NOT NULL,
    DROP COLUMN market_phase,
    DROP COLUMN risk_level,
    DROP COLUMN headline,
    DROP COLUMN summary,
    DROP COLUMN market_analysis,
    DROP COLUMN action_summary,
    DROP COLUMN caution_message,
    DROP CONSTRAINT tb_investment_briefing_briefing_date_key,
    ADD CONSTRAINT uk_investment_briefing_date UNIQUE (briefing_date);

COMMENT ON TABLE tb_investment_briefing IS '일자별 투자 브리핑 기본정보';
COMMENT ON COLUMN tb_investment_briefing.briefing_id IS '투자 브리핑 고유 식별자';
COMMENT ON COLUMN tb_investment_briefing.briefing_date IS '브리핑 기준일';
COMMENT ON COLUMN tb_investment_briefing.title IS '브리핑 제목';
COMMENT ON COLUMN tb_investment_briefing.briefing_status IS '브리핑 상태. 기본값: DRAFT';
COMMENT ON COLUMN tb_investment_briefing.created_at IS '레코드 생성 일시';
COMMENT ON COLUMN tb_investment_briefing.updated_at IS '레코드 최종 수정 일시';
