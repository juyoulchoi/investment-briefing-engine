\set ON_ERROR_STOP on
BEGIN;
DO $$ DECLARE t text; definition text; BEGIN
 IF current_database() NOT LIKE 'investment_unified_%' THEN RAISE EXCEPTION 'Candidate database required'; END IF;
 FOREACH t IN ARRAY ARRAY['tb_hold','tb_prc_day','tb_krx_data_row','tb_krx_clct_job','tb_krx_clct_job_item','tb_krx_bf_job','tb_krx_bf_day','tb_mkt_cal'] LOOP
  IF to_regclass(format('v1.%I',t)) IS NOT NULL THEN
   definition:=pg_get_viewdef(format('v1.%I',t)::regclass,true);
   definition:=replace(definition,'v1."TB_HOLD"','portfolio."TB_HOLD"');
   EXECUTE format('CREATE OR REPLACE VIEW source.%I AS %s',t,definition);
  END IF;
 END LOOP;
 CREATE OR REPLACE VIEW v1.tb_krx_bf_day AS
 SELECT "ID" AS id,"BF_JOB_ID" AS backfill_job_id,"BASE_DT" AS base_date,"ST" AS status,
        "SKIP_RSN" AS skip_reason,"CLCT_JOB_ID" AS collection_job_id,"RETRY_DATA_CDS" AS retry_dataset_codes,
        "ATTEMPT_CNT" AS attempt_count,"ERR_MSG" AS error_message,"START_DT" AS started_at,
        "END_DT" AS completed_at,"UPD_DT" AS updated_at,"ORIG_CLCT_JOB_ID" AS original_collection_job_id
 FROM source."TB_KRX_BF_DAY";
 CREATE OR REPLACE VIEW source.tb_krx_bf_day AS SELECT * FROM v1.tb_krx_bf_day;
 IF to_regclass('v1."TB_CLCT_RETRY_EVT"') IS NOT NULL AND to_regclass('source."TB_CLCT_RETRY_EVT"') IS NULL THEN
  ALTER TABLE v1."TB_CLCT_RETRY_EVT" SET SCHEMA source;
 END IF;
 IF to_regclass('v2."TB_CLCT_RETRY_EVT"') IS NOT NULL THEN
  ALTER TABLE v2."TB_CLCT_RETRY_EVT" SET SCHEMA archive_v2;
 END IF;
END $$;
COMMIT;
