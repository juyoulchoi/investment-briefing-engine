-- 추가매수 대기현금의 기준 원장은 TB_CASH_RSV이다.
-- 기존 계좌 복제 잔액을 원장과 일치시켜 대시보드 및 투자판단 계산의 불일치를 해소한다.
UPDATE "TB_ACCT" a
SET "RSV_CASH_AMT" = r."RSV_AMT",
    "UPD_DTTM" = CURRENT_TIMESTAMP
FROM "TB_CASH_RSV" r
WHERE r."ACCT_ID" = a."ACCT_ID"
  AND a."RSV_CASH_AMT" IS DISTINCT FROM r."RSV_AMT";
