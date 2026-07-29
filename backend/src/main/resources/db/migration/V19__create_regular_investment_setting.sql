CREATE TABLE tb_regular_investment_setting (
    setting_id    BIGSERIAL PRIMARY KEY,

    account_type  VARCHAR(30) NOT NULL,
    stock_code    VARCHAR(30) NOT NULL,
    stock_name    VARCHAR(100) NOT NULL,

    cycle_type    VARCHAR(20) NOT NULL,
    day_of_week   VARCHAR(20),
    day_of_month  INTEGER,

    amount        NUMERIC(18, 2),
    quantity      NUMERIC(18, 6),

    active_yn     CHAR(1) NOT NULL DEFAULT 'Y',
    pause_reason  TEXT,

    start_date    DATE,
    end_date      DATE,

    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_regular_investment_account_type
        FOREIGN KEY (account_type)
        REFERENCES tb_account_type_code (account_type),
    CONSTRAINT ck_regular_investment_active_yn
        CHECK (active_yn IN ('Y', 'N')),
    CONSTRAINT ck_regular_investment_day_of_month
        CHECK (day_of_month IS NULL OR day_of_month BETWEEN 1 AND 31),
    CONSTRAINT ck_regular_investment_date_range
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

COMMENT ON TABLE tb_regular_investment_setting IS '계좌 및 종목별 정기투자 설정';
COMMENT ON COLUMN tb_regular_investment_setting.setting_id IS '정기투자 설정 고유 식별자';
COMMENT ON COLUMN tb_regular_investment_setting.account_type IS '정기투자를 실행할 계좌 유형 코드';
COMMENT ON COLUMN tb_regular_investment_setting.stock_code IS '정기투자 대상 국내외 주식 종목 코드';
COMMENT ON COLUMN tb_regular_investment_setting.stock_name IS '정기투자 대상 종목 명칭';
COMMENT ON COLUMN tb_regular_investment_setting.cycle_type IS '정기투자 주기 유형';
COMMENT ON COLUMN tb_regular_investment_setting.day_of_week IS '정기투자 실행 요일';
COMMENT ON COLUMN tb_regular_investment_setting.day_of_month IS '정기투자 실행 일자: 1~31';
COMMENT ON COLUMN tb_regular_investment_setting.amount IS '회차별 정기투자 금액';
COMMENT ON COLUMN tb_regular_investment_setting.quantity IS '회차별 정기투자 수량';
COMMENT ON COLUMN tb_regular_investment_setting.active_yn IS '정기투자 활성 여부: Y 또는 N';
COMMENT ON COLUMN tb_regular_investment_setting.pause_reason IS '정기투자 중지 사유';
COMMENT ON COLUMN tb_regular_investment_setting.start_date IS '정기투자 시작일';
COMMENT ON COLUMN tb_regular_investment_setting.end_date IS '정기투자 종료일';
COMMENT ON COLUMN tb_regular_investment_setting.created_at IS '설정 생성 일시';
COMMENT ON COLUMN tb_regular_investment_setting.updated_at IS '설정 최종 수정 일시';
