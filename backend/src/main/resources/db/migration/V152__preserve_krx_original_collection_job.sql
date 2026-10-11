DO $$ BEGIN
 IF to_regclass('public."TB_KRX_BF_DAY"') IS NOT NULL THEN
  ALTER TABLE public."TB_KRX_BF_DAY" ADD COLUMN IF NOT EXISTS "ORIG_CLCT_JOB_ID" UUID;
  EXECUTE 'CREATE OR REPLACE VIEW public.tb_krx_bf_day AS
   SELECT "ID" AS id,"BF_JOB_ID" AS backfill_job_id,"BASE_DT" AS base_date,"ST" AS status,
          "SKIP_RSN" AS skip_reason,"CLCT_JOB_ID" AS collection_job_id,"RETRY_DATA_CDS" AS retry_dataset_codes,
          "ATTEMPT_CNT" AS attempt_count,"ERR_MSG" AS error_message,"START_DT" AS started_at,
          "END_DT" AS completed_at,"UPD_DT" AS updated_at,"ORIG_CLCT_JOB_ID" AS original_collection_job_id
   FROM public."TB_KRX_BF_DAY"';
 END IF;
END $$;
