CREATE TABLE tb_domestic_stock (
    stock_code   VARCHAR(30) PRIMARY KEY,
    stock_name   VARCHAR(100) NOT NULL,
    asset_type   VARCHAR(20) NOT NULL,
    active_yn    CHAR(1) NOT NULL DEFAULT 'Y',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_domestic_stock_asset_type CHECK (asset_type IN ('STOCK', 'ETF')),
    CONSTRAINT ck_domestic_stock_active_yn CHECK (active_yn IN ('Y', 'N'))
);

COMMENT ON TABLE tb_domestic_stock IS '사용자가 보유한 국내주식 및 국내 상장 ETF';
COMMENT ON COLUMN tb_domestic_stock.stock_code IS '국내주식 또는 ETF 단축 종목코드';
COMMENT ON COLUMN tb_domestic_stock.stock_name IS '국내주식 또는 ETF 종목명';
COMMENT ON COLUMN tb_domestic_stock.asset_type IS '국내자산 유형: STOCK 또는 ETF';
COMMENT ON COLUMN tb_domestic_stock.active_yn IS '보유 종목 사용 여부: Y 또는 N';
COMMENT ON COLUMN tb_domestic_stock.created_at IS '보유 종목 등록 일시';
COMMENT ON COLUMN tb_domestic_stock.updated_at IS '보유 종목 최종 수정 일시';

INSERT INTO tb_domestic_stock (stock_code, stock_name, asset_type)
VALUES
    ('007340', 'DN오토모티브', 'STOCK'),
    ('105560', 'KB금융', 'STOCK'),
    ('010120', 'LS ELECTRIC', 'STOCK'),
    ('001440', '대한전선', 'STOCK'),
    ('034020', '두산에너빌리티', 'STOCK'),
    ('058470', '리노공업', 'STOCK'),
    ('083650', '비에이치아이', 'STOCK'),
    ('028050', '삼성E&A', 'STOCK'),
    ('005930', '삼성전자', 'STOCK'),
    ('032820', '우리기술', 'STOCK'),
    ('000100', '유한양행', 'STOCK'),
    ('014680', '한솔케미칼', 'STOCK'),
    ('000720', '현대건설', 'STOCK'),
    ('000660', 'SK하이닉스', 'STOCK'),
    ('298040', '효성중공업', 'STOCK'),
    ('411060', 'ACE KRX금현물', 'ETF'),
    ('069500', 'KODEX 200', 'ETF'),
    ('305720', 'KODEX 2차전지산업', 'ETF'),
    ('471990', 'KODEX AI반도체핵심장비', 'ETF'),
    ('117700', 'KODEX 건설', 'ETF'),
    ('487230', 'KODEX 미국AI전력핵심인프라', 'ETF'),
    ('0089D0', 'KODEX 금융고배당TOP10', 'ETF'),
    ('379800', 'KODEX 미국S&P500', 'ETF'),
    ('144600', 'KODEX 은선물(H)', 'ETF'),
    ('266420', 'KODEX 헬스케어', 'ETF'),
    ('161510', 'PLUS 고배당주', 'ETF'),
    ('455890', 'RISE 머니마켓액티브', 'ETF'),
    ('0023A0', 'SOL 미국양자컴퓨팅TOP10', 'ETF'),
    ('0051G0', 'SOL 미국원자력SMR', 'ETF'),
    ('139270', 'TIGER 200 금융', 'ETF'),
    ('227550', 'TIGER 200 산업재', 'ETF'),
    ('463250', 'TIGER K방산&우주', 'ETF'),
    ('160580', 'TIGER 구리실물', 'ETF'),
    ('464310', 'TIGER 글로벌AI&로보틱스INDXX', 'ETF'),
    ('329200', 'TIGER 리츠부동산인프라', 'ETF'),
    ('458730', 'TIGER 미국배당다우존스', 'ETF'),
    ('0183J0', 'TIGER 미국우주테크', 'ETF'),
    ('305080', 'TIGER 미국채10년', 'ETF'),
    ('0046A0', 'TIGER 미국초단기국채', 'ETF'),
    ('381180', 'TIGER 미국필라델피아반도체나스닥', 'ETF'),
    ('466940', 'TIGER 은행고배당플러스TOP10', 'ETF'),
    ('241180', 'TIGER 일본니케이225', 'ETF'),
    ('494670', 'TIGER 조선TOP10', 'ETF'),
    ('302190', 'TIGER 중장기국채', 'ETF');
