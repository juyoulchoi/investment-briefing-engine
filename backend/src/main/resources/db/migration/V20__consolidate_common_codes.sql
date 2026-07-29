CREATE TABLE tb_common_code (
    code_group    VARCHAR(50) NOT NULL,
    code_key      VARCHAR(50) NOT NULL,
    code_name     VARCHAR(100) NOT NULL,
    description   VARCHAR(300),
    display_order INTEGER NOT NULL DEFAULT 0,
    active_yn     CHAR(1) NOT NULL DEFAULT 'Y',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (code_group, code_key),
    CONSTRAINT ck_common_code_active_yn CHECK (active_yn IN ('Y', 'N'))
);

COMMENT ON TABLE tb_common_code IS '투자 브리핑 시스템 통합 공통 코드';
COMMENT ON COLUMN tb_common_code.code_group IS '공통 코드 그룹키';
COMMENT ON COLUMN tb_common_code.code_key IS '그룹 내 공통 코드키';
COMMENT ON COLUMN tb_common_code.code_name IS '공통 코드 표시 명칭';
COMMENT ON COLUMN tb_common_code.description IS '공통 코드 상세 설명';
COMMENT ON COLUMN tb_common_code.display_order IS '그룹 내 화면 표시 순서';
COMMENT ON COLUMN tb_common_code.active_yn IS '공통 코드 사용 여부: Y 또는 N';
COMMENT ON COLUMN tb_common_code.created_at IS '공통 코드 생성 일시';
COMMENT ON COLUMN tb_common_code.updated_at IS '공통 코드 최종 수정 일시';

INSERT INTO tb_common_code
    (code_group, code_key, code_name, description, display_order, active_yn)
SELECT 'MARKET_INDICATOR', market_code, market_name,
       '자산 유형: ' || asset_type, display_order, CASE WHEN active THEN 'Y' ELSE 'N' END
FROM tb_market_indicator_code;

INSERT INTO tb_common_code
    (code_group, code_key, code_name, description, display_order, active_yn)
SELECT 'ACCOUNT_TYPE', account_type, account_name,
       '투자 계좌 유형', display_order, CASE WHEN active THEN 'Y' ELSE 'N' END
FROM tb_account_type_code;

INSERT INTO tb_common_code
    (code_group, code_key, code_name, description, display_order, active_yn)
SELECT 'TRAFFIC_LIGHT', traffic_light, code_name,
       '보유 종목 투자 신호등', display_order, CASE WHEN active THEN 'Y' ELSE 'N' END
FROM tb_traffic_light_code;

INSERT INTO tb_common_code
    (code_group, code_key, code_name, description, display_order, active_yn)
SELECT 'ACTION_SIGNAL', action_signal, code_name,
       '보유 종목 투자 행동 신호', display_order, CASE WHEN active THEN 'Y' ELSE 'N' END
FROM tb_action_signal_code;

INSERT INTO tb_common_code
    (code_group, code_key, code_name, description, display_order, active_yn)
SELECT 'REBUY_SIGNAL', rebuy_signal, code_name,
       '보유 종목 재매수 신호', display_order, CASE WHEN active THEN 'Y' ELSE 'N' END
FROM tb_rebuy_signal_code;

ALTER TABLE tb_briefing_market_indicator
    DROP CONSTRAINT fk_briefing_indicator_market_code,
    ADD COLUMN market_code_group VARCHAR(50) NOT NULL DEFAULT 'MARKET_INDICATOR',
    ADD CONSTRAINT ck_briefing_indicator_market_group
        CHECK (market_code_group = 'MARKET_INDICATOR'),
    ADD CONSTRAINT fk_briefing_indicator_common_code
        FOREIGN KEY (market_code_group, market_code)
        REFERENCES tb_common_code (code_group, code_key);

COMMENT ON COLUMN tb_briefing_market_indicator.market_code_group IS '시장 지표 공통 코드 그룹키: MARKET_INDICATOR';

ALTER TABLE tb_briefing_account_strategy
    DROP CONSTRAINT fk_account_strategy_account_type,
    ADD COLUMN account_type_group VARCHAR(50) NOT NULL DEFAULT 'ACCOUNT_TYPE',
    ADD CONSTRAINT ck_account_strategy_type_group
        CHECK (account_type_group = 'ACCOUNT_TYPE'),
    ADD CONSTRAINT fk_account_strategy_common_code
        FOREIGN KEY (account_type_group, account_type)
        REFERENCES tb_common_code (code_group, code_key);

COMMENT ON COLUMN tb_briefing_account_strategy.account_type_group IS '계좌 유형 공통 코드 그룹키: ACCOUNT_TYPE';

ALTER TABLE tb_briefing_stock_signal
    DROP CONSTRAINT fk_stock_signal_account_type,
    DROP CONSTRAINT fk_stock_signal_traffic_light,
    DROP CONSTRAINT fk_stock_signal_action,
    DROP CONSTRAINT fk_stock_signal_rebuy,
    ADD COLUMN account_type_group VARCHAR(50) NOT NULL DEFAULT 'ACCOUNT_TYPE',
    ADD COLUMN traffic_light_group VARCHAR(50) NOT NULL DEFAULT 'TRAFFIC_LIGHT',
    ADD COLUMN action_signal_group VARCHAR(50) NOT NULL DEFAULT 'ACTION_SIGNAL',
    ADD COLUMN rebuy_signal_group VARCHAR(50) NOT NULL DEFAULT 'REBUY_SIGNAL',
    ADD CONSTRAINT ck_stock_signal_account_group
        CHECK (account_type_group = 'ACCOUNT_TYPE'),
    ADD CONSTRAINT ck_stock_signal_traffic_group
        CHECK (traffic_light_group = 'TRAFFIC_LIGHT'),
    ADD CONSTRAINT ck_stock_signal_action_group
        CHECK (action_signal_group = 'ACTION_SIGNAL'),
    ADD CONSTRAINT ck_stock_signal_rebuy_group
        CHECK (rebuy_signal_group = 'REBUY_SIGNAL'),
    ADD CONSTRAINT fk_stock_signal_account_common_code
        FOREIGN KEY (account_type_group, account_type)
        REFERENCES tb_common_code (code_group, code_key),
    ADD CONSTRAINT fk_stock_signal_traffic_common_code
        FOREIGN KEY (traffic_light_group, traffic_light)
        REFERENCES tb_common_code (code_group, code_key),
    ADD CONSTRAINT fk_stock_signal_action_common_code
        FOREIGN KEY (action_signal_group, action_signal)
        REFERENCES tb_common_code (code_group, code_key),
    ADD CONSTRAINT fk_stock_signal_rebuy_common_code
        FOREIGN KEY (rebuy_signal_group, rebuy_signal)
        REFERENCES tb_common_code (code_group, code_key);

COMMENT ON COLUMN tb_briefing_stock_signal.account_type_group IS '계좌 유형 공통 코드 그룹키: ACCOUNT_TYPE';
COMMENT ON COLUMN tb_briefing_stock_signal.traffic_light_group IS '투자 신호등 공통 코드 그룹키: TRAFFIC_LIGHT';
COMMENT ON COLUMN tb_briefing_stock_signal.action_signal_group IS '행동 신호 공통 코드 그룹키: ACTION_SIGNAL';
COMMENT ON COLUMN tb_briefing_stock_signal.rebuy_signal_group IS '재매수 신호 공통 코드 그룹키: REBUY_SIGNAL';

ALTER TABLE tb_regular_investment_setting
    DROP CONSTRAINT fk_regular_investment_account_type,
    ADD COLUMN account_type_group VARCHAR(50) NOT NULL DEFAULT 'ACCOUNT_TYPE',
    ADD CONSTRAINT ck_regular_investment_account_group
        CHECK (account_type_group = 'ACCOUNT_TYPE'),
    ADD CONSTRAINT fk_regular_investment_account_common_code
        FOREIGN KEY (account_type_group, account_type)
        REFERENCES tb_common_code (code_group, code_key);

COMMENT ON COLUMN tb_regular_investment_setting.account_type_group IS '계좌 유형 공통 코드 그룹키: ACCOUNT_TYPE';

DROP TABLE tb_market_indicator_code;
DROP TABLE tb_account_type_code;
DROP TABLE tb_traffic_light_code;
DROP TABLE tb_action_signal_code;
DROP TABLE tb_rebuy_signal_code;
