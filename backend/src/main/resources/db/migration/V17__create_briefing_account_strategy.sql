CREATE TABLE tb_account_type_code (
    account_type   VARCHAR(30) PRIMARY KEY,
    account_name   VARCHAR(100) NOT NULL,
    display_order  INTEGER NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE tb_account_type_code IS '투자 브리핑에서 사용하는 계좌 유형 코드';
COMMENT ON COLUMN tb_account_type_code.account_type IS '계좌 유형 코드';
COMMENT ON COLUMN tb_account_type_code.account_name IS '계좌 유형 표시 명칭';
COMMENT ON COLUMN tb_account_type_code.display_order IS '화면 표시 순서';
COMMENT ON COLUMN tb_account_type_code.active IS '계좌 유형 코드 사용 여부';
COMMENT ON COLUMN tb_account_type_code.created_at IS '코드 생성 일시';
COMMENT ON COLUMN tb_account_type_code.updated_at IS '코드 최종 수정 일시';

INSERT INTO tb_account_type_code
    (account_type, account_name, display_order)
VALUES
    ('GENERAL',  '종합계좌',          1),
    ('ISA',      'ISA계좌',           2),
    ('PENSION',  '연금계좌',          3),
    ('DOMESTIC', '국내주식 별도계좌', 4);

CREATE TABLE tb_briefing_account_strategy (
    account_strategy_id  BIGSERIAL PRIMARY KEY,
    briefing_id          BIGINT NOT NULL,

    account_type         VARCHAR(30) NOT NULL,
    account_name         VARCHAR(100),

    market_signal        VARCHAR(30),
    regular_buy_signal   VARCHAR(30),
    additional_buy_signal VARCHAR(30),
    cash_strategy        VARCHAR(30),

    invest_amount        NUMERIC(18, 2),
    cash_balance         NUMERIC(18, 2),
    cash_ratio           NUMERIC(10, 4),

    strategy_summary     TEXT,
    caution_message      TEXT,

    CONSTRAINT fk_account_strategy_briefing
        FOREIGN KEY (briefing_id)
        REFERENCES tb_investment_briefing (briefing_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_account_strategy_account_type
        FOREIGN KEY (account_type)
        REFERENCES tb_account_type_code (account_type)
);

COMMENT ON TABLE tb_briefing_account_strategy IS '투자 브리핑별 계좌 운용 전략';
COMMENT ON COLUMN tb_briefing_account_strategy.account_strategy_id IS '계좌 전략 고유 식별자';
COMMENT ON COLUMN tb_briefing_account_strategy.briefing_id IS '투자 브리핑 식별자';
COMMENT ON COLUMN tb_briefing_account_strategy.account_type IS '계좌 유형 코드';
COMMENT ON COLUMN tb_briefing_account_strategy.account_name IS '사용자 계좌 표시 명칭';
COMMENT ON COLUMN tb_briefing_account_strategy.market_signal IS '계좌별 시장 대응 신호';
COMMENT ON COLUMN tb_briefing_account_strategy.regular_buy_signal IS '정기매수 실행 신호';
COMMENT ON COLUMN tb_briefing_account_strategy.additional_buy_signal IS '추가매수 실행 신호';
COMMENT ON COLUMN tb_briefing_account_strategy.cash_strategy IS '현금 운용 전략 코드';
COMMENT ON COLUMN tb_briefing_account_strategy.invest_amount IS '권장 투자 금액';
COMMENT ON COLUMN tb_briefing_account_strategy.cash_balance IS '계좌 현금 잔액';
COMMENT ON COLUMN tb_briefing_account_strategy.cash_ratio IS '계좌 내 현금 비율';
COMMENT ON COLUMN tb_briefing_account_strategy.strategy_summary IS '계좌 운용 전략 요약';
COMMENT ON COLUMN tb_briefing_account_strategy.caution_message IS '계좌 운용 시 주의사항';
