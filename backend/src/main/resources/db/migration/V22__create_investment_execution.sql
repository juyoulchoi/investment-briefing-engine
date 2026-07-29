INSERT INTO tb_common_code
    (code_group, code_key, code_name, description, display_order, active_yn)
VALUES
    ('EXECUTION_SOURCE_TYPE', 'REGULAR',   '정기매수',      '정기투자 설정에 따른 투자 실행',       1, 'Y'),
    ('EXECUTION_SOURCE_TYPE', 'BRIEFING',  '브리핑 신호',   '투자 브리핑 신호에 따른 투자 실행',   2, 'Y'),
    ('EXECUTION_SOURCE_TYPE', 'MANUAL',    '사용자 직접 판단', '사용자가 직접 판단하여 수행한 투자 실행', 3, 'Y'),
    ('EXECUTION_SOURCE_TYPE', 'REBALANCE', '리밸런싱',      '포트폴리오 리밸런싱을 위한 투자 실행', 4, 'Y');

CREATE TABLE tb_investment_execution (
    execution_id             BIGSERIAL PRIMARY KEY,
    briefing_id              BIGINT,

    execution_date           DATE NOT NULL,
    account_type_group       VARCHAR(50) NOT NULL DEFAULT 'ACCOUNT_TYPE',
    account_type             VARCHAR(30) NOT NULL,
    stock_code               VARCHAR(30) NOT NULL,
    stock_name               VARCHAR(100) NOT NULL,

    action_type              VARCHAR(20) NOT NULL,
    quantity                 NUMERIC(18, 6),
    price                    NUMERIC(18, 4),
    amount                   NUMERIC(18, 2),

    execution_status         VARCHAR(20),
    source_type_group        VARCHAR(50) NOT NULL DEFAULT 'EXECUTION_SOURCE_TYPE',
    source_type              VARCHAR(20),
    memo                     TEXT,

    created_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_execution_briefing
        FOREIGN KEY (briefing_id)
        REFERENCES tb_investment_briefing (briefing_id),
    CONSTRAINT ck_execution_account_type_group
        CHECK (account_type_group = 'ACCOUNT_TYPE'),
    CONSTRAINT fk_execution_account_type_common_code
        FOREIGN KEY (account_type_group, account_type)
        REFERENCES tb_common_code (code_group, code_key),
    CONSTRAINT ck_execution_source_type_group
        CHECK (source_type_group = 'EXECUTION_SOURCE_TYPE'),
    CONSTRAINT fk_execution_source_type_common_code
        FOREIGN KEY (source_type_group, source_type)
        REFERENCES tb_common_code (code_group, code_key)
);

COMMENT ON TABLE tb_investment_execution IS '계좌 및 종목별 실제 투자 실행 내역';
COMMENT ON COLUMN tb_investment_execution.execution_id IS '투자 실행 내역 고유 식별자';
COMMENT ON COLUMN tb_investment_execution.briefing_id IS '투자 실행의 근거가 된 브리핑 식별자';
COMMENT ON COLUMN tb_investment_execution.execution_date IS '투자 주문 또는 체결 실행일';
COMMENT ON COLUMN tb_investment_execution.account_type_group IS '계좌 유형 공통 코드 그룹키: ACCOUNT_TYPE';
COMMENT ON COLUMN tb_investment_execution.account_type IS '투자 실행 계좌 유형 코드';
COMMENT ON COLUMN tb_investment_execution.stock_code IS '투자 실행 대상 국내외 주식 종목 코드';
COMMENT ON COLUMN tb_investment_execution.stock_name IS '투자 실행 대상 종목 명칭';
COMMENT ON COLUMN tb_investment_execution.action_type IS '투자 실행 행동 유형';
COMMENT ON COLUMN tb_investment_execution.quantity IS '투자 실행 수량';
COMMENT ON COLUMN tb_investment_execution.price IS '투자 실행 단가';
COMMENT ON COLUMN tb_investment_execution.amount IS '투자 실행 총금액';
COMMENT ON COLUMN tb_investment_execution.execution_status IS '투자 주문 또는 체결 상태';
COMMENT ON COLUMN tb_investment_execution.source_type_group IS '실행 출처 공통 코드 그룹키: EXECUTION_SOURCE_TYPE';
COMMENT ON COLUMN tb_investment_execution.source_type IS '투자 실행 출처 코드: REGULAR, BRIEFING, MANUAL, REBALANCE';
COMMENT ON COLUMN tb_investment_execution.memo IS '투자 실행 관련 메모';
COMMENT ON COLUMN tb_investment_execution.created_at IS '투자 실행 내역 생성 일시';
