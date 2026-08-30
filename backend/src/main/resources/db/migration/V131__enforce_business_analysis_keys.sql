-- Keep surrogate PKs where rows are referenced internally, and enforce the
-- domain analysis key separately so retries cannot create duplicate business rows.

ALTER TABLE "TB_BRF_ACCT_STRG"
  ADD CONSTRAINT "UK_TB_BRF_ACCT_STRG_01"
  UNIQUE ("BRF_ID", "ACCT_TP_GRP", "ACCT_TP");

ALTER TABLE "TB_BRF_MKT_IND"
  ADD CONSTRAINT "UK_TB_BRF_MKT_IND_01"
  UNIQUE ("BRF_ID", "MKT_CD_GRP", "MKT_CD");

-- Evidence can have multiple rows of the same type and may not have a URL.
-- EVDC_KEY is therefore supplied by the producer from the stable source identity.
ALTER TABLE "TB_BRF_EVDC" ADD COLUMN "EVDC_KEY" VARCHAR(200);
UPDATE "TB_BRF_EVDC" SET "EVDC_KEY" = 'LEGACY-' || "EVDC_ID" WHERE "EVDC_KEY" IS NULL;
ALTER TABLE "TB_BRF_EVDC"
  ALTER COLUMN "EVDC_KEY" SET NOT NULL,
  ADD CONSTRAINT "UK_TB_BRF_EVDC_01" UNIQUE ("BRF_ID", "EVDC_KEY");

-- An execution date/account/stock/amount tuple is not unique because partial
-- fills are valid. The caller must provide one stable key per order/execution.
ALTER TABLE "TB_INV_EXEC" ADD COLUMN "IDEMP_KEY" VARCHAR(100);
UPDATE "TB_INV_EXEC" SET "IDEMP_KEY" = 'LEGACY-' || "EXEC_ID" WHERE "IDEMP_KEY" IS NULL;
ALTER TABLE "TB_INV_EXEC"
  ALTER COLUMN "IDEMP_KEY" SET NOT NULL,
  ADD CONSTRAINT "UK_TB_INV_EXEC_01" UNIQUE ("IDEMP_KEY");

ALTER TABLE "TB_CLCT_RETRY_EVT"
  ADD CONSTRAINT "UK_TB_CLCT_RETRY_EVT_01"
  UNIQUE ("PROVIDER_CD", "RETRY_ROOT_JOB_ID", "RETRY_NO");

ALTER TABLE "TB_DATA_CORR_HIS"
  ADD CONSTRAINT "UK_TB_DATA_CORR_HIS_01"
  UNIQUE ("PROVIDER_CD", "DATASET_CD", "SOURCE_KEY", "BASE_DT", "PAYLOAD_VER");

ALTER TABLE "TB_SCH_LOG"
  ADD CONSTRAINT "UK_TB_SCH_LOG_01" UNIQUE ("TRACE_ID");

CREATE OR REPLACE VIEW tb_brf_evdc AS
SELECT
    "EVDC_ID" AS "evidence_id",
    "BRF_ID" AS "briefing_id",
    "EVDC_TP_GRP" AS "evidence_type_group",
    "EVDC_TP" AS "evidence_type",
    "TTL" AS "title",
    "SRC_NM" AS "source_name",
    "SRC_URL" AS "source_url",
    "PUB_DT" AS "published_at",
    "SUM" AS "summary",
    "IMP_DIR" AS "impact_direction",
    "IMP_LVL" AS "impact_level",
    "REL_CODES" AS "related_codes",
    "EVDC_KEY" AS "evidence_key"
FROM "TB_BRF_EVDC";

CREATE OR REPLACE VIEW tb_inv_exec AS
SELECT
    "EXEC_ID" AS "execution_id",
    "BRF_ID" AS "briefing_id",
    "EXEC_DT" AS "execution_date",
    "ACCT_TP_GRP" AS "account_type_group",
    "ACCT_TP" AS "account_type",
    "STK_CD" AS "stock_code",
    "STK_NM" AS "stock_name",
    "ACT_TP" AS "action_type",
    "QTY" AS "quantity",
    "PRC" AS "price",
    "AMT" AS "amount",
    "EXEC_ST" AS "execution_status",
    "SRC_TP_GRP" AS "source_type_group",
    "SRC_TP" AS "source_type",
    "MEMO" AS "memo",
    "REG_DT" AS "created_at",
    "IDEMP_KEY" AS "idempotency_key"
FROM "TB_INV_EXEC";

COMMENT ON CONSTRAINT "UK_TB_BRF_ACCT_STRG_01" ON "TB_BRF_ACCT_STRG"
  IS '브리핑별 계좌유형 운용전략 업무 분석키';
COMMENT ON CONSTRAINT "UK_TB_BRF_MKT_IND_01" ON "TB_BRF_MKT_IND"
  IS '브리핑별 시장지표 업무 분석키';
COMMENT ON COLUMN "TB_BRF_EVDC"."EVDC_KEY"
  IS '브리핑 내 근거자료의 안정적인 업무 식별키(URL, 공급자 문서ID 또는 원문 해시 기반)';
COMMENT ON COLUMN "TB_INV_EXEC"."IDEMP_KEY"
  IS '주문 또는 체결 중복 등록을 방지하는 호출자 생성 멱등키';
COMMENT ON CONSTRAINT "UK_TB_CLCT_RETRY_EVT_01" ON "TB_CLCT_RETRY_EVT"
  IS '공급자별 재시도 루트 작업과 재시도 순번 업무키';
COMMENT ON CONSTRAINT "UK_TB_DATA_CORR_HIS_01" ON "TB_DATA_CORR_HIS"
  IS '공급자 원천행의 기준일 및 payload 버전별 정정 업무키';
COMMENT ON CONSTRAINT "UK_TB_SCH_LOG_01" ON "TB_SCH_LOG"
  IS '스케줄러 실행 추적 식별키';
