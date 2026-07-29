CREATE TABLE tb_investment_briefing (
    briefing_id       BIGSERIAL PRIMARY KEY,
    briefing_date     DATE NOT NULL UNIQUE,

    market_phase      VARCHAR(30),
    risk_level        VARCHAR(20),

    headline          VARCHAR(300),
    summary           TEXT,
    market_analysis   TEXT,
    action_summary    TEXT,
    caution_message   TEXT,

    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE tb_investment_briefing IS '일자별 투자 브리핑';
COMMENT ON COLUMN tb_investment_briefing.market_phase IS '시장 국면: 정상, 과열, 조정, 강한조정, 폭락';
COMMENT ON COLUMN tb_investment_briefing.risk_level IS '위험 수준: LOW, MEDIUM, HIGH, VERY_HIGH';
