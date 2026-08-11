-- 화면, API, 서비스 및 배치에서 사용하지 않는 미도입 투자등급 전략 테이블을 제거한다.
-- FK 종속 순서에 따라 계좌별 종목 전략 테이블을 먼저 삭제한다.
DROP TABLE IF EXISTS "TB_ACCT_ITEM_INV_STG";
DROP TABLE IF EXISTS "TB_INV_GRD";
