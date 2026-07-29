CREATE TABLE tb_market_indicator_code (
    market_code    VARCHAR(30) PRIMARY KEY,
    market_name    VARCHAR(100) NOT NULL,
    asset_type     VARCHAR(30) NOT NULL,
    display_order  INTEGER NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE tb_market_indicator_code IS '투자 브리핑에서 사용하는 시장 지표 코드';
COMMENT ON COLUMN tb_market_indicator_code.market_code IS '시장 지표 코드';
COMMENT ON COLUMN tb_market_indicator_code.market_name IS '시장 지표 표시 명칭';
COMMENT ON COLUMN tb_market_indicator_code.asset_type IS '지표 자산 유형: INDEX, FX, BOND, COMMODITY, VOLATILITY';
COMMENT ON COLUMN tb_market_indicator_code.display_order IS '화면 표시 순서';
COMMENT ON COLUMN tb_market_indicator_code.active IS '시장 지표 코드 사용 여부';
COMMENT ON COLUMN tb_market_indicator_code.created_at IS '코드 생성 일시';
COMMENT ON COLUMN tb_market_indicator_code.updated_at IS '코드 최종 수정 일시';

INSERT INTO tb_market_indicator_code
    (market_code, market_name, asset_type, display_order)
VALUES
    ('KOSPI',   '코스피',             'INDEX',       1),
    ('KOSDAQ',  '코스닥',             'INDEX',       2),
    ('SP500',   'S&P 500',            'INDEX',       3),
    ('NASDAQ',  '나스닥 종합지수',     'INDEX',       4),
    ('DOW',     '다우존스 산업평균지수', 'INDEX',       5),
    ('USD_KRW', '원·달러 환율',        'FX',          6),
    ('US10Y',   '미국 10년물 국채금리', 'BOND',        7),
    ('WTI',     'WTI 원유',            'COMMODITY',   8),
    ('GOLD',    '금',                  'COMMODITY',   9),
    ('VIX',     '변동성 지수',          'VOLATILITY', 10);

ALTER TABLE tb_briefing_market_indicator
    ADD CONSTRAINT fk_briefing_indicator_market_code
    FOREIGN KEY (market_code)
    REFERENCES tb_market_indicator_code (market_code);
