INSERT INTO tb_common_code
    (code_group, code_key, code_name, description, display_order, active_yn)
VALUES
    ('CYCLE_TYPE', 'DAILY',   '매일',      '매 거래일 또는 매일 정기투자를 실행하는 주기', 1, 'Y'),
    ('CYCLE_TYPE', 'WEEKLY',  '매주',      '지정한 요일에 정기투자를 실행하는 주기',       2, 'Y'),
    ('CYCLE_TYPE', 'MONTHLY', '매월',      '지정한 일자에 정기투자를 실행하는 주기',       3, 'Y'),
    ('CYCLE_TYPE', 'MANUAL',  '수동',      '자동 일정 없이 사용자가 직접 투자하는 방식',   4, 'Y'),
    ('CYCLE_TYPE', 'PAUSED',  '일시정지',  '정기투자 실행이 일시적으로 중지된 상태',       5, 'Y');

ALTER TABLE tb_regular_investment_setting
    ADD COLUMN cycle_type_group VARCHAR(50) NOT NULL DEFAULT 'CYCLE_TYPE',
    ADD CONSTRAINT ck_regular_investment_cycle_group
        CHECK (cycle_type_group = 'CYCLE_TYPE'),
    ADD CONSTRAINT fk_regular_investment_cycle_common_code
        FOREIGN KEY (cycle_type_group, cycle_type)
        REFERENCES tb_common_code (code_group, code_key);

COMMENT ON COLUMN tb_regular_investment_setting.cycle_type_group IS '정기투자 주기 공통 코드 그룹키: CYCLE_TYPE';
COMMENT ON COLUMN tb_regular_investment_setting.cycle_type IS '정기투자 주기 코드: DAILY, WEEKLY, MONTHLY, MANUAL, PAUSED';
