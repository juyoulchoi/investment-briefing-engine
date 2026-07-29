CREATE TABLE tb_traffic_light_code (
    traffic_light  VARCHAR(20) PRIMARY KEY,
    code_name      VARCHAR(100) NOT NULL,
    display_order  INTEGER NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE tb_traffic_light_code IS '보유 종목 투자 신호등 코드';
COMMENT ON COLUMN tb_traffic_light_code.traffic_light IS '투자 신호등 코드';
COMMENT ON COLUMN tb_traffic_light_code.code_name IS '투자 신호등 설명';
COMMENT ON COLUMN tb_traffic_light_code.display_order IS '화면 표시 순서';
COMMENT ON COLUMN tb_traffic_light_code.active IS '코드 사용 여부';

INSERT INTO tb_traffic_light_code
    (traffic_light, code_name, display_order)
VALUES
    ('GREEN',  '정기매수 유지',          1),
    ('BLUE',   '관망 또는 보유',         2),
    ('YELLOW', '주의, 추가매수 제한',    3),
    ('RED',    '매수중지 또는 비중축소', 4);

CREATE TABLE tb_action_signal_code (
    action_signal  VARCHAR(30) PRIMARY KEY,
    code_name      VARCHAR(100) NOT NULL,
    display_order  INTEGER NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE tb_action_signal_code IS '보유 종목 투자 행동 신호 코드';
COMMENT ON COLUMN tb_action_signal_code.action_signal IS '투자 행동 신호 코드';
COMMENT ON COLUMN tb_action_signal_code.code_name IS '투자 행동 신호 설명';
COMMENT ON COLUMN tb_action_signal_code.display_order IS '화면 표시 순서';
COMMENT ON COLUMN tb_action_signal_code.active IS '코드 사용 여부';

INSERT INTO tb_action_signal_code
    (action_signal, code_name, display_order)
VALUES
    ('REGULAR_BUY',    '정기매수',        1),
    ('SMALL_BUY',      '소액 추가매수',   2),
    ('ADDITIONAL_BUY', '추가매수',        3),
    ('HOLD',           '보유',            4),
    ('WAIT',           '관망',            5),
    ('PAUSE_BUY',      '매수 일시정지',   6),
    ('REDUCE',         '비중축소',        7),
    ('TAKE_PROFIT',    '수익실현',        8),
    ('SELL',           '매도',            9);

CREATE TABLE tb_rebuy_signal_code (
    rebuy_signal   VARCHAR(30) PRIMARY KEY,
    code_name      VARCHAR(100) NOT NULL,
    display_order  INTEGER NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE tb_rebuy_signal_code IS '보유 종목 재매수 신호 코드';
COMMENT ON COLUMN tb_rebuy_signal_code.rebuy_signal IS '재매수 신호 코드';
COMMENT ON COLUMN tb_rebuy_signal_code.code_name IS '재매수 신호 설명';
COMMENT ON COLUMN tb_rebuy_signal_code.display_order IS '화면 표시 순서';
COMMENT ON COLUMN tb_rebuy_signal_code.active IS '코드 사용 여부';

INSERT INTO tb_rebuy_signal_code
    (rebuy_signal, code_name, display_order)
VALUES
    ('NOT_READY',   '재매수 조건 미충족', 1),
    ('WATCH',       '재매수 조건 관찰',   2),
    ('PARTIAL_BUY', '분할 매수 가능',     3),
    ('BUY_ALLOWED', '재매수 가능',        4),
    ('STRONG_BUY',  '적극 매수 신호',     5);

CREATE TABLE tb_briefing_stock_signal (
    stock_signal_id        BIGSERIAL PRIMARY KEY,
    briefing_id            BIGINT NOT NULL,

    account_type           VARCHAR(30) NOT NULL,
    stock_code             VARCHAR(30) NOT NULL,
    stock_name             VARCHAR(100) NOT NULL,
    market_type            VARCHAR(20),

    current_price          NUMERIC(18, 4),
    profit_rate            NUMERIC(10, 4),
    portfolio_ratio        NUMERIC(10, 4),
    target_ratio           NUMERIC(10, 4),

    risk_grade             VARCHAR(20),
    traffic_light          VARCHAR(20),
    action_signal          VARCHAR(30),
    regular_buy_status     VARCHAR(30),
    rebuy_signal           VARCHAR(30),

    recommended_buy_amount NUMERIC(18, 2),
    recommended_sell_ratio NUMERIC(10, 4),

    priority               INTEGER,
    signal_reason          TEXT,
    memo                   TEXT,

    CONSTRAINT fk_stock_signal_briefing
        FOREIGN KEY (briefing_id)
        REFERENCES tb_investment_briefing (briefing_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_stock_signal_account_type
        FOREIGN KEY (account_type)
        REFERENCES tb_account_type_code (account_type),
    CONSTRAINT fk_stock_signal_traffic_light
        FOREIGN KEY (traffic_light)
        REFERENCES tb_traffic_light_code (traffic_light),
    CONSTRAINT fk_stock_signal_action
        FOREIGN KEY (action_signal)
        REFERENCES tb_action_signal_code (action_signal),
    CONSTRAINT fk_stock_signal_rebuy
        FOREIGN KEY (rebuy_signal)
        REFERENCES tb_rebuy_signal_code (rebuy_signal)
);

COMMENT ON TABLE tb_briefing_stock_signal IS '투자 브리핑별 보유 종목 투자 신호';
COMMENT ON COLUMN tb_briefing_stock_signal.stock_signal_id IS '종목 투자 신호 고유 식별자';
COMMENT ON COLUMN tb_briefing_stock_signal.briefing_id IS '투자 브리핑 식별자';
COMMENT ON COLUMN tb_briefing_stock_signal.account_type IS '보유 계좌 유형 코드';
COMMENT ON COLUMN tb_briefing_stock_signal.stock_code IS '국내외 주식 종목 코드';
COMMENT ON COLUMN tb_briefing_stock_signal.stock_name IS '주식 종목 명칭';
COMMENT ON COLUMN tb_briefing_stock_signal.market_type IS '거래 시장 유형';
COMMENT ON COLUMN tb_briefing_stock_signal.current_price IS '신호 생성 시점 현재가';
COMMENT ON COLUMN tb_briefing_stock_signal.profit_rate IS '보유 종목 손익률';
COMMENT ON COLUMN tb_briefing_stock_signal.portfolio_ratio IS '현재 포트폴리오 편입 비율';
COMMENT ON COLUMN tb_briefing_stock_signal.target_ratio IS '목표 포트폴리오 편입 비율';
COMMENT ON COLUMN tb_briefing_stock_signal.risk_grade IS '종목 위험 등급';
COMMENT ON COLUMN tb_briefing_stock_signal.traffic_light IS '투자 신호등 코드: GREEN, BLUE, YELLOW, RED';
COMMENT ON COLUMN tb_briefing_stock_signal.action_signal IS '투자 행동 신호 코드';
COMMENT ON COLUMN tb_briefing_stock_signal.regular_buy_status IS '정기매수 현재 상태';
COMMENT ON COLUMN tb_briefing_stock_signal.rebuy_signal IS '재매수 신호 코드';
COMMENT ON COLUMN tb_briefing_stock_signal.recommended_buy_amount IS '권장 매수 금액';
COMMENT ON COLUMN tb_briefing_stock_signal.recommended_sell_ratio IS '권장 매도 비율';
COMMENT ON COLUMN tb_briefing_stock_signal.priority IS '투자 행동 우선순위';
COMMENT ON COLUMN tb_briefing_stock_signal.signal_reason IS '투자 신호 산출 근거';
COMMENT ON COLUMN tb_briefing_stock_signal.memo IS '종목 투자 신호 메모';
