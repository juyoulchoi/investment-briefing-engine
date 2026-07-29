INSERT INTO tb_common_code
    (code_group, code_key, code_name, description, display_order, active_yn)
VALUES
    ('EVIDENCE_TYPE', 'NEWS',               '뉴스',       '시장과 종목에 영향을 주는 언론 및 뉴스 자료',       1, 'Y'),
    ('EVIDENCE_TYPE', 'ECONOMIC_INDICATOR', '경제지표',   '경기, 물가, 고용 등 거시경제 지표 자료',            2, 'Y'),
    ('EVIDENCE_TYPE', 'EARNINGS',           '기업실적',   '기업의 매출, 이익 및 실적 전망 자료',               3, 'Y'),
    ('EVIDENCE_TYPE', 'DISCLOSURE',         '공시',       '기업 및 금융시장 공식 공시 자료',                   4, 'Y'),
    ('EVIDENCE_TYPE', 'POLICY',             '정책',       '정부와 중앙은행의 법령, 정책 및 통화정책 자료',     5, 'Y'),
    ('EVIDENCE_TYPE', 'MARKET_FLOW',        '시장수급',   '투자자별 매매와 자금 흐름 관련 자료',               6, 'Y'),
    ('EVIDENCE_TYPE', 'TECHNICAL',          '기술적 분석', '가격, 거래량 및 기술적 지표를 이용한 분석 자료',    7, 'Y');

CREATE TABLE tb_briefing_evidence (
    evidence_id        BIGSERIAL PRIMARY KEY,
    briefing_id        BIGINT NOT NULL,

    evidence_type_group VARCHAR(50) NOT NULL DEFAULT 'EVIDENCE_TYPE',
    evidence_type      VARCHAR(30) NOT NULL,
    title              VARCHAR(500),
    source_name        VARCHAR(100),
    source_url         TEXT,
    published_at       TIMESTAMP,

    summary            TEXT,
    impact_direction   VARCHAR(20),
    impact_level       VARCHAR(20),
    related_codes      TEXT,

    CONSTRAINT fk_evidence_briefing
        FOREIGN KEY (briefing_id)
        REFERENCES tb_investment_briefing (briefing_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_evidence_type_group
        CHECK (evidence_type_group = 'EVIDENCE_TYPE'),
    CONSTRAINT fk_evidence_type_common_code
        FOREIGN KEY (evidence_type_group, evidence_type)
        REFERENCES tb_common_code (code_group, code_key)
);

COMMENT ON TABLE tb_briefing_evidence IS '투자 브리핑 판단의 근거 자료';
COMMENT ON COLUMN tb_briefing_evidence.evidence_id IS '브리핑 근거 자료 고유 식별자';
COMMENT ON COLUMN tb_briefing_evidence.briefing_id IS '투자 브리핑 식별자';
COMMENT ON COLUMN tb_briefing_evidence.evidence_type_group IS '근거 유형 공통 코드 그룹키: EVIDENCE_TYPE';
COMMENT ON COLUMN tb_briefing_evidence.evidence_type IS '근거 자료 유형 코드';
COMMENT ON COLUMN tb_briefing_evidence.title IS '근거 자료 제목';
COMMENT ON COLUMN tb_briefing_evidence.source_name IS '근거 자료 제공처 명칭';
COMMENT ON COLUMN tb_briefing_evidence.source_url IS '근거 자료 원문 URL';
COMMENT ON COLUMN tb_briefing_evidence.published_at IS '근거 자료 게시 일시';
COMMENT ON COLUMN tb_briefing_evidence.summary IS '근거 자료 핵심 요약';
COMMENT ON COLUMN tb_briefing_evidence.impact_direction IS '시장 또는 종목에 대한 영향 방향';
COMMENT ON COLUMN tb_briefing_evidence.impact_level IS '시장 또는 종목에 대한 영향 수준';
COMMENT ON COLUMN tb_briefing_evidence.related_codes IS '근거 자료와 관련된 시장 또는 종목 코드 목록';
