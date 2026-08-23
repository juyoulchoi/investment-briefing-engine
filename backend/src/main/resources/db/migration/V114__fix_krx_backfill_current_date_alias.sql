DROP VIEW tb_krx_bf_job;

CREATE VIEW tb_krx_bf_job AS
SELECT
    "ID" id,
    "FROM_DT" from_date,
    "TO_DT" to_date,
    "ST" status,
    "DATA_CDS" dataset_codes,
    "REQ_INTERVAL_MS" request_interval_ms,
    "TOT_DAY_CNT" total_day_count,
    "SUCCESS_DAY_CNT" success_day_count,
    "FAILED_DAY_CNT" failed_day_count,
    "SKIP_DAY_CNT" skipped_day_count,
    "PENDING_DAY_CNT" pending_day_count,
    "CURR_DT" current_base_date,
    "ERR_MSG" error_message,
    "REG_DT" created_at,
    "START_DT" started_at,
    "END_DT" completed_at,
    "UPD_DT" updated_at
FROM "TB_KRX_BF_JOB";

COMMENT ON VIEW tb_krx_bf_job IS 'KRX 기간 백필 상위 작업 호환 view';
