DROP VIEW IF EXISTS vw_account_stock;

CREATE TABLE TB_ACCT (
    ACCT_TP       VARCHAR(30) NOT NULL,
    ACCT_NM       VARCHAR(100) NOT NULL,
    TOTAL_AMT     NUMERIC(18,2) NOT NULL DEFAULT 0,
    AVAIL_CASH    NUMERIC(18,2) NOT NULL DEFAULT 0,
    CURRENCY      VARCHAR(10) NOT NULL DEFAULT 'KRW',
    USE_YN        CHAR(1) NOT NULL DEFAULT 'Y',
    REG_DT        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MOD_DT        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_ACCT PRIMARY KEY (ACCT_TP),
    CONSTRAINT CK_ACCT_01 CHECK (ACCT_TP IN ('GENERAL', 'DOMESTIC', 'ISA', 'PENSION')),
    CONSTRAINT CK_ACCT_02 CHECK (USE_YN IN ('Y', 'N')),
    CONSTRAINT CK_ACCT_03 CHECK (TOTAL_AMT >= 0 AND AVAIL_CASH >= 0)
);

INSERT INTO TB_ACCT (ACCT_TP, ACCT_NM, CURRENCY)
VALUES
    ('GENERAL', '종합계좌', 'USD'),
    ('DOMESTIC', '국내주식계좌', 'KRW'),
    ('ISA', 'ISA계좌', 'KRW'),
    ('PENSION', '연금계좌', 'KRW');

COMMENT ON TABLE TB_ACCT IS '계좌 기본정보와 평가금액 및 가용현금을 관리하는 테이블';
COMMENT ON COLUMN TB_ACCT.ACCT_TP IS '계좌 유형 코드: GENERAL, DOMESTIC, ISA, PENSION';
COMMENT ON COLUMN TB_ACCT.ACCT_NM IS '계좌 명칭';
COMMENT ON COLUMN TB_ACCT.TOTAL_AMT IS '계좌 총 평가금액';
COMMENT ON COLUMN TB_ACCT.AVAIL_CASH IS '계좌에서 투자 가능한 현금';
COMMENT ON COLUMN TB_ACCT.CURRENCY IS '계좌 기준 통화';
COMMENT ON COLUMN TB_ACCT.USE_YN IS '계좌 사용 여부';
COMMENT ON COLUMN TB_ACCT.REG_DT IS '최초 등록 일시';
COMMENT ON COLUMN TB_ACCT.MOD_DT IS '최종 수정 일시';

ALTER TABLE tb_stock RENAME TO TB_HOLD;
ALTER TABLE TB_HOLD
    ADD COLUMN QTY NUMERIC(18,6) NOT NULL DEFAULT 0,
    ADD COLUMN AVG_PRC NUMERIC(18,4) NOT NULL DEFAULT 0,
    ADD CONSTRAINT FK_HOLD_01 FOREIGN KEY (market_scope)
        REFERENCES TB_ACCT (ACCT_TP),
    ADD CONSTRAINT CK_HOLD_01 CHECK (QTY >= 0 AND AVG_PRC >= 0);

CREATE TABLE TB_STK_SET (
    ACCT_TP       VARCHAR(30) NOT NULL,
    STK_CD        VARCHAR(30) NOT NULL,
    STK_GRADE     VARCHAR(30),
    TARGET_WEIGHT NUMERIC(10,6),
    BENCHMARK_CD  VARCHAR(50),
    REG_DT        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MOD_DT        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_STK_SET PRIMARY KEY (ACCT_TP, STK_CD),
    CONSTRAINT FK_STK_SET_01 FOREIGN KEY (ACCT_TP, STK_CD)
        REFERENCES TB_HOLD (market_scope, stock_code) ON DELETE CASCADE,
    CONSTRAINT CK_STK_SET_01 CHECK (
        STK_GRADE IS NULL OR STK_GRADE IN ('CORE', 'SATELLITE', 'THEME', 'CASH_LIKE')
    ),
    CONSTRAINT CK_STK_SET_02 CHECK (
        TARGET_WEIGHT IS NULL OR (TARGET_WEIGHT >= 0 AND TARGET_WEIGHT <= 1)
    )
);

INSERT INTO TB_STK_SET (ACCT_TP, STK_CD, STK_GRADE)
SELECT market_scope, stock_code, stock_grade
FROM TB_HOLD;

ALTER TABLE TB_HOLD
    DROP CONSTRAINT ck_stock_grade,
    DROP COLUMN stock_grade;

COMMENT ON TABLE TB_HOLD IS '계좌별 보유종목, 수량 및 평균매입단가를 관리하는 테이블';
COMMENT ON COLUMN TB_HOLD.market_scope IS '계좌 유형 코드';
COMMENT ON COLUMN TB_HOLD.stock_code IS '종목 코드';
COMMENT ON COLUMN TB_HOLD.stock_name IS '종목 명칭';
COMMENT ON COLUMN TB_HOLD.QTY IS '보유 수량';
COMMENT ON COLUMN TB_HOLD.AVG_PRC IS '평균 매입단가';
COMMENT ON TABLE TB_STK_SET IS '계좌별 종목등급, 목표비중 및 기준지수를 관리하는 테이블';
COMMENT ON COLUMN TB_STK_SET.ACCT_TP IS '계좌 유형 코드';
COMMENT ON COLUMN TB_STK_SET.STK_CD IS '종목 코드';
COMMENT ON COLUMN TB_STK_SET.STK_GRADE IS '종목등급: CORE, SATELLITE, THEME, CASH_LIKE';
COMMENT ON COLUMN TB_STK_SET.TARGET_WEIGHT IS '계좌 내 목표 비중. 0부터 1 사이';
COMMENT ON COLUMN TB_STK_SET.BENCHMARK_CD IS '수익률 비교에 사용할 기준지수 코드';

ALTER TABLE tb_regular_investment_setting RENAME TO TB_BUY_SET;
ALTER TABLE TB_BUY_SET
    DROP CONSTRAINT fk_regular_investment_account_common_code,
    DROP CONSTRAINT fk_regular_investment_cycle_common_code,
    DROP CONSTRAINT ck_regular_investment_account_group,
    DROP CONSTRAINT ck_regular_investment_cycle_group,
    DROP COLUMN account_type_group,
    DROP COLUMN cycle_type_group,
    ADD CONSTRAINT FK_BUY_SET_01 FOREIGN KEY (account_type, stock_code)
        REFERENCES TB_HOLD (market_scope, stock_code) ON DELETE CASCADE,
    ADD CONSTRAINT CK_BUY_SET_01 CHECK (
        account_type IN ('GENERAL', 'DOMESTIC', 'ISA', 'PENSION')
    ),
    ADD CONSTRAINT CK_BUY_SET_02 CHECK (
        cycle_type IN (
            'DAILY', 'WEEKLY', 'MONTHLY', 'MANUAL', 'PAUSED',
            'MONDAY', 'FRIDAY', 'MON_WED_FRI', 'MONTHLY_15'
        )
    );

COMMENT ON TABLE TB_BUY_SET IS '계좌별 종목 모으기주기, 정기매수금액 및 일시정지 상태를 관리하는 테이블';
COMMENT ON COLUMN TB_BUY_SET.account_type IS '계좌 유형 코드';
COMMENT ON COLUMN TB_BUY_SET.stock_code IS '정기매수 대상 종목 코드';
COMMENT ON COLUMN TB_BUY_SET.cycle_type IS '모으기 주기 코드';
COMMENT ON COLUMN TB_BUY_SET.amount IS '회차별 정기매수 금액';
COMMENT ON COLUMN TB_BUY_SET.active_yn IS '정기매수 활성 여부';
COMMENT ON COLUMN TB_BUY_SET.pause_reason IS '일시정지 사유';

ALTER TABLE tb_overseas_stock_daily_price RENAME TO TB_STK_PRC;
COMMENT ON TABLE TB_STK_PRC IS '종목별 일일 시세를 저장하는 테이블';
COMMENT ON COLUMN TB_STK_PRC.symbol IS '종목 코드 또는 해외주식 티커';
COMMENT ON COLUMN TB_STK_PRC.trading_day IS '거래일';
COMMENT ON COLUMN TB_STK_PRC.open_price IS '시가';
COMMENT ON COLUMN TB_STK_PRC.high_price IS '고가';
COMMENT ON COLUMN TB_STK_PRC.low_price IS '저가';
COMMENT ON COLUMN TB_STK_PRC.close_price IS '종가';
COMMENT ON COLUMN TB_STK_PRC.adjusted_close IS '수정 종가';
COMMENT ON COLUMN TB_STK_PRC.volume IS '거래량';

ALTER TABLE tb_daily_market_data RENAME TO TB_IDX_PRC;
ALTER TABLE TB_IDX_PRC
    ADD COLUMN open_value NUMERIC(20,6),
    ADD COLUMN high_value NUMERIC(20,6),
    ADD COLUMN low_value NUMERIC(20,6),
    ADD COLUMN trading_volume NUMERIC(20,2),
    ADD COLUMN trading_value NUMERIC(20,2),
    ADD COLUMN market_cap NUMERIC(20,2);

INSERT INTO TB_IDX_PRC (
    trade_date, indicator_code, indicator_name, close_value,
    change_value, change_rate, source_name, collected_at,
    open_value, high_value, low_value, trading_volume, trading_value, market_cap
)
SELECT
    base_date, index_name, index_name, close_price,
    change_amount, change_rate, 'KRX', updated_at,
    open_price, high_price, low_price, trading_volume, trading_value, market_cap
FROM tb_krx_index_daily_price
ON CONFLICT (trade_date, indicator_code) DO UPDATE
SET close_value = EXCLUDED.close_value,
    change_value = EXCLUDED.change_value,
    change_rate = EXCLUDED.change_rate,
    source_name = EXCLUDED.source_name,
    collected_at = EXCLUDED.collected_at,
    open_value = EXCLUDED.open_value,
    high_value = EXCLUDED.high_value,
    low_value = EXCLUDED.low_value,
    trading_volume = EXCLUDED.trading_volume,
    trading_value = EXCLUDED.trading_value,
    market_cap = EXCLUDED.market_cap;

DROP TABLE tb_krx_index_daily_price;

COMMENT ON TABLE TB_IDX_PRC IS '기준지수별 일일 시세와 거래정보를 저장하는 테이블';
COMMENT ON COLUMN TB_IDX_PRC.trade_date IS '기준지수 거래일';
COMMENT ON COLUMN TB_IDX_PRC.indicator_code IS '기준지수 코드';
COMMENT ON COLUMN TB_IDX_PRC.indicator_name IS '기준지수 명칭';
COMMENT ON COLUMN TB_IDX_PRC.close_value IS '종가 또는 지표 마감값';
COMMENT ON COLUMN TB_IDX_PRC.change_rate IS '전일 대비 등락률';

CREATE TABLE TB_RISK_RULE (
    RULE_ID         BIGSERIAL NOT NULL,
    RULE_NM         VARCHAR(100) NOT NULL,
    MIN_DROP_RT     NUMERIC(10,4),
    MAX_DROP_RT     NUMERIC(10,4),
    RISK_SCORE      INTEGER NOT NULL DEFAULT 0,
    CASH_INPUT_RT   NUMERIC(10,4) NOT NULL DEFAULT 0,
    USE_YN          CHAR(1) NOT NULL DEFAULT 'Y',
    REG_DT          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MOD_DT          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_RISK_RULE PRIMARY KEY (RULE_ID),
    CONSTRAINT UK_RISK_RULE_01 UNIQUE (RULE_NM),
    CONSTRAINT CK_RISK_RULE_01 CHECK (RISK_SCORE >= 0),
    CONSTRAINT CK_RISK_RULE_02 CHECK (CASH_INPUT_RT >= 0 AND CASH_INPUT_RT <= 1),
    CONSTRAINT CK_RISK_RULE_03 CHECK (USE_YN IN ('Y', 'N')),
    CONSTRAINT CK_RISK_RULE_04 CHECK (
        MAX_DROP_RT IS NULL OR MIN_DROP_RT IS NULL OR MIN_DROP_RT <= MAX_DROP_RT
    )
);

COMMENT ON TABLE TB_RISK_RULE IS '하락률 구간별 위험점수와 현금투입비율을 설정하는 테이블';
COMMENT ON COLUMN TB_RISK_RULE.MIN_DROP_RT IS '위험규칙 적용 최소 하락률';
COMMENT ON COLUMN TB_RISK_RULE.MAX_DROP_RT IS '위험규칙 적용 최대 하락률';
COMMENT ON COLUMN TB_RISK_RULE.RISK_SCORE IS '조건 충족 시 부여할 위험점수';
COMMENT ON COLUMN TB_RISK_RULE.CASH_INPUT_RT IS '조건 충족 시 현금 투입비율';

CREATE TABLE TB_ANALYSIS (
    ANALYSIS_ID       BIGSERIAL NOT NULL,
    ANALYSIS_DT       DATE NOT NULL,
    ACCT_TP           VARCHAR(30) NOT NULL,
    STK_CD            VARCHAR(30) NOT NULL,
    MARKET_VALUE      NUMERIC(18,2) NOT NULL DEFAULT 0,
    CURRENT_WEIGHT    NUMERIC(10,6) NOT NULL DEFAULT 0,
    WEIGHT_ST         VARCHAR(20) NOT NULL,
    MARKET_PHASE      VARCHAR(30) NOT NULL,
    RISK_LEVEL        VARCHAR(20) NOT NULL,
    REGULAR_BUY_SIG   VARCHAR(30) NOT NULL,
    ADD_BUY_AMT       NUMERIC(18,2) NOT NULL DEFAULT 0,
    REBUY_SIG         VARCHAR(20) NOT NULL,
    FINAL_ACTION      VARCHAR(30) NOT NULL,
    REG_DT            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MOD_DT            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_ANALYSIS PRIMARY KEY (ANALYSIS_ID),
    CONSTRAINT UK_ANALYSIS_01 UNIQUE (ANALYSIS_DT, ACCT_TP, STK_CD),
    CONSTRAINT FK_ANALYSIS_01 FOREIGN KEY (ACCT_TP, STK_CD)
        REFERENCES TB_HOLD (market_scope, stock_code) ON DELETE CASCADE,
    CONSTRAINT CK_ANALYSIS_01 CHECK (WEIGHT_ST IN ('UNDER', 'NORMAL', 'OVER')),
    CONSTRAINT CK_ANALYSIS_02 CHECK (
        MARKET_PHASE IN ('NORMAL', 'CORRECTION', 'STRONG_CORRECTION', 'CRASH')
    ),
    CONSTRAINT CK_ANALYSIS_03 CHECK (
        RISK_LEVEL IN ('LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH')
    ),
    CONSTRAINT CK_ANALYSIS_04 CHECK (
        REGULAR_BUY_SIG IN (
            'EXECUTE', 'NOT_SCHEDULED', 'PAUSED', 'OVER_WEIGHT',
            'INSUFFICIENT_CASH', 'RISK_BLOCKED'
        )
    ),
    CONSTRAINT CK_ANALYSIS_05 CHECK (REBUY_SIG IN ('WAIT', 'WATCH', 'PARTIAL', 'ALLOWED')),
    CONSTRAINT CK_ANALYSIS_06 CHECK (
        FINAL_ACTION IN (
            'STOP_BUY', 'REDUCE_WEIGHT', 'ADDITIONAL_BUY',
            'REGULAR_BUY', 'REBUY_PARTIAL', 'HOLD'
        )
    )
);

COMMENT ON TABLE TB_ANALYSIS IS '일자별 계좌·종목 분석 결과를 저장하는 테이블';
COMMENT ON COLUMN TB_ANALYSIS.ANALYSIS_DT IS '분석 기준일';
COMMENT ON COLUMN TB_ANALYSIS.ACCT_TP IS '계좌 유형 코드';
COMMENT ON COLUMN TB_ANALYSIS.STK_CD IS '분석 대상 종목 코드';
COMMENT ON COLUMN TB_ANALYSIS.MARKET_VALUE IS '분석 시점 종목 평가금액';
COMMENT ON COLUMN TB_ANALYSIS.CURRENT_WEIGHT IS '분석 시점 계좌 내 종목 비중';
COMMENT ON COLUMN TB_ANALYSIS.WEIGHT_ST IS '목표비중 대비 상태';
COMMENT ON COLUMN TB_ANALYSIS.MARKET_PHASE IS '시장 하락 국면';
COMMENT ON COLUMN TB_ANALYSIS.RISK_LEVEL IS '최종 위험등급';
COMMENT ON COLUMN TB_ANALYSIS.REGULAR_BUY_SIG IS '정기매수 실행 신호';
COMMENT ON COLUMN TB_ANALYSIS.ADD_BUY_AMT IS '추천 추가매수 금액';
COMMENT ON COLUMN TB_ANALYSIS.REBUY_SIG IS '재매수 신호';
COMMENT ON COLUMN TB_ANALYSIS.FINAL_ACTION IS '최종 행동 신호';

CREATE VIEW vw_account_stock AS
SELECT
    h.market_scope,
    scope_code.code_name AS market_scope_name,
    h.stock_code,
    h.stock_name,
    ss.STK_GRADE AS stock_grade,
    grade_code.code_name AS stock_grade_name,
    h.listing_scope,
    listing_code.code_name AS listing_scope_name,
    h.asset_type,
    h.exchange_name,
    h.currency,
    h.provider,
    h.active_yn,
    h.created_at,
    h.updated_at
FROM TB_HOLD h
LEFT JOIN TB_STK_SET ss
  ON ss.ACCT_TP = h.market_scope AND ss.STK_CD = h.stock_code
JOIN tb_common_code scope_code
  ON scope_code.code_group = 'MARKET_SCOPE'
 AND scope_code.code_key = h.market_scope
LEFT JOIN tb_common_code grade_code
  ON grade_code.code_group = 'STOCK_GRADE'
 AND grade_code.code_key = ss.STK_GRADE
JOIN tb_common_code listing_code
  ON listing_code.code_group = 'LISTING_SCOPE'
 AND listing_code.code_key = h.listing_scope;
