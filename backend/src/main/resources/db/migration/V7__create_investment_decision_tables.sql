CREATE TABLE IF NOT EXISTS tb_investment_decision (
    id BIGSERIAL PRIMARY KEY,
    decision_date DATE NOT NULL,
    market_regime VARCHAR(40) NOT NULL,
    market_score INTEGER NOT NULL,
    sentiment_phase VARCHAR(40) NOT NULL,
    sentiment_risk_score INTEGER NOT NULL,
    total_minimum_buy_amount BIGINT NOT NULL,
    total_recommended_buy_amount BIGINT NOT NULL,
    newly_reserved_cash BIGINT NOT NULL,
    available_additional_buy_cash BIGINT NOT NULL,
    request_payload JSONB NOT NULL,
    result_payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS ix_investment_decision_date ON tb_investment_decision (decision_date DESC);

CREATE TABLE IF NOT EXISTS tb_investment_stock_decision (
    id BIGSERIAL PRIMARY KEY,
    investment_decision_id BIGINT NOT NULL REFERENCES tb_investment_decision(id) ON DELETE CASCADE,
    account VARCHAR(100) NOT NULL,
    stock_code VARCHAR(30) NOT NULL,
    stock_name VARCHAR(200) NOT NULL,
    action_signal VARCHAR(40) NOT NULL,
    multiplier NUMERIC(8, 3) NOT NULL,
    minimum_buy_amount BIGINT NOT NULL,
    recommended_buy_amount BIGINT NOT NULL,
    reserved_cash BIGINT NOT NULL,
    reasons JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS ix_stock_decision_parent ON tb_investment_stock_decision (investment_decision_id);
CREATE INDEX IF NOT EXISTS ix_stock_decision_code ON tb_investment_stock_decision (stock_code, created_at DESC);

COMMENT ON TABLE tb_investment_decision IS '투자 의사결정 실행 이력';
COMMENT ON TABLE tb_investment_stock_decision IS '투자 의사결정별 종목 행동 신호';
