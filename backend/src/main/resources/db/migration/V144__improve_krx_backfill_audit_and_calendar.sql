ALTER TABLE "TB_KRX_BF_DAY"
    ADD COLUMN "ORIG_CLCT_JOB_ID" UUID REFERENCES "TB_KRX_CLCT_JOB"("ID");

UPDATE "TB_KRX_BF_DAY"
SET "ORIG_CLCT_JOB_ID" = "CLCT_JOB_ID"
WHERE "CLCT_JOB_ID" IS NOT NULL;

DROP VIEW tb_krx_bf_day;
CREATE VIEW tb_krx_bf_day AS
SELECT
    "ID" id,
    "BF_JOB_ID" backfill_job_id,
    "BASE_DT" base_date,
    "ST" status,
    "SKIP_RSN" skip_reason,
    "CLCT_JOB_ID" collection_job_id,
    "ORIG_CLCT_JOB_ID" original_collection_job_id,
    "RETRY_DATA_CDS" retry_dataset_codes,
    "ATTEMPT_CNT" attempt_count,
    "ERR_MSG" error_message,
    "START_DT" started_at,
    "END_DT" completed_at,
    "UPD_DT" updated_at
FROM "TB_KRX_BF_DAY";

COMMENT ON COLUMN "TB_KRX_BF_DAY"."ORIG_CLCT_JOB_ID" IS
    '최초 실행한 날짜별 KRX 수집 Job ID. 재처리 시에도 유지되는 감사 연결 키';

INSERT INTO "TB_MKT_CAL"("CAL_DT", "MKT_CD", "OPEN_YN", "HLDY_NM") VALUES
('2024-01-01','KRX','N','신정'),
('2024-02-09','KRX','N','설날 연휴'),
('2024-02-12','KRX','N','설날 대체공휴일'),
('2024-03-01','KRX','N','삼일절'),
('2024-04-10','KRX','N','국회의원 선거일'),
('2024-05-01','KRX','N','근로자의 날'),
('2024-05-06','KRX','N','어린이날 대체공휴일'),
('2024-05-15','KRX','N','부처님오신날'),
('2024-06-06','KRX','N','현충일'),
('2024-08-15','KRX','N','광복절'),
('2024-09-16','KRX','N','추석 연휴'),
('2024-09-17','KRX','N','추석'),
('2024-09-18','KRX','N','추석 연휴'),
('2024-10-01','KRX','N','국군의 날 임시공휴일'),
('2024-10-03','KRX','N','개천절'),
('2024-10-09','KRX','N','한글날'),
('2024-12-25','KRX','N','성탄절'),
('2024-12-31','KRX','N','연말 휴장일'),
('2025-01-01','KRX','N','신정'),
('2025-01-27','KRX','N','임시공휴일'),
('2025-01-28','KRX','N','설날 연휴'),
('2025-01-29','KRX','N','설날'),
('2025-01-30','KRX','N','설날 연휴'),
('2025-03-03','KRX','N','삼일절 대체공휴일'),
('2025-05-01','KRX','N','근로자의 날'),
('2025-05-05','KRX','N','어린이날·부처님오신날'),
('2025-05-06','KRX','N','대체공휴일'),
('2025-06-03','KRX','N','대통령 선거일'),
('2025-06-06','KRX','N','현충일'),
('2025-08-15','KRX','N','광복절'),
('2025-10-03','KRX','N','개천절'),
('2025-10-06','KRX','N','추석'),
('2025-10-07','KRX','N','추석 연휴'),
('2025-10-08','KRX','N','추석 대체공휴일'),
('2025-10-09','KRX','N','한글날'),
('2025-12-25','KRX','N','성탄절'),
('2025-12-31','KRX','N','연말 휴장일'),
('2026-01-01','KRX','N','신정'),
('2026-02-16','KRX','N','설날 연휴'),
('2026-02-17','KRX','N','설날'),
('2026-02-18','KRX','N','설날 연휴'),
('2026-03-02','KRX','N','삼일절 대체공휴일'),
('2026-05-01','KRX','N','근로자의 날'),
('2026-05-05','KRX','N','어린이날'),
('2026-05-25','KRX','N','부처님오신날 대체공휴일'),
('2026-06-03','KRX','N','전국동시지방선거일'),
('2026-07-17','KRX','N','제헌절'),
('2026-08-17','KRX','N','광복절 대체공휴일')
ON CONFLICT ("CAL_DT", "MKT_CD") DO NOTHING;

UPDATE "TB_KRX_BF_DAY" d
SET "ST"='SKIPPED',
    "SKIP_RSN"=COALESCE(c."HLDY_NM", 'KRX 시장 달력 휴장일'),
    "ERR_MSG"=NULL,
    "UPD_DT"=CURRENT_TIMESTAMP
FROM "TB_MKT_CAL" c
WHERE d."BASE_DT"=c."CAL_DT"
  AND c."MKT_CD"='KRX'
  AND c."OPEN_YN"='N'
  AND d."ST"='FAILED'
  AND NOT EXISTS (
      SELECT 1 FROM "TB_KRX_CLCT_JOB_ITEM" i
      WHERE i."JOB_ID"=d."CLCT_JOB_ID"
        AND i."ST" IN ('COLLECTION_FAILED','NOT_AUTHORIZED'));

UPDATE "TB_KRX_BF_JOB" j SET
  "SUCCESS_DAY_CNT"=(SELECT count(*) FROM "TB_KRX_BF_DAY" d WHERE d."BF_JOB_ID"=j."ID" AND d."ST"='SUCCESS'),
  "FAILED_DAY_CNT"=(SELECT count(*) FROM "TB_KRX_BF_DAY" d WHERE d."BF_JOB_ID"=j."ID" AND d."ST"='FAILED'),
  "SKIP_DAY_CNT"=(SELECT count(*) FROM "TB_KRX_BF_DAY" d WHERE d."BF_JOB_ID"=j."ID" AND d."ST"='SKIPPED'),
  "PENDING_DAY_CNT"=(SELECT count(*) FROM "TB_KRX_BF_DAY" d WHERE d."BF_JOB_ID"=j."ID" AND d."ST"='PENDING'),
  "ST"=CASE
    WHEN j."ST" IN ('COMPLETED','COMPLETED_WITH_ERRORS') AND NOT EXISTS (
      SELECT 1 FROM "TB_KRX_BF_DAY" d WHERE d."BF_JOB_ID"=j."ID" AND d."ST"='FAILED')
    THEN 'COMPLETED'
    ELSE j."ST"
  END,
  "UPD_DT"=CURRENT_TIMESTAMP;
