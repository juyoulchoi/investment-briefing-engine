ALTER TABLE "TB_ACCT" RENAME COLUMN "acct_tp" TO "ACCT_TP";
ALTER TABLE "TB_ACCT" RENAME COLUMN "acct_nm" TO "ACCT_NM";
ALTER TABLE "TB_ACCT" RENAME COLUMN "total_amt" TO "TOT_AMT";
ALTER TABLE "TB_ACCT" RENAME COLUMN "avail_cash" TO "AVAIL_CASH";
ALTER TABLE "TB_ACCT" RENAME COLUMN "currency" TO "CURR";
ALTER TABLE "TB_ACCT" RENAME COLUMN "use_yn" TO "USE_YN";
ALTER TABLE "TB_ACCT" RENAME COLUMN "reg_dt" TO "REG_DT";
ALTER TABLE "TB_ACCT" RENAME COLUMN "mod_dt" TO "MOD_DT";

ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "analysis_id" TO "ANLYS_ID";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "analysis_dt" TO "ANLYS_DT";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "acct_tp" TO "ACCT_TP";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "stk_cd" TO "STK_CD";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "market_value" TO "MKT_VAL";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "current_weight" TO "CUR_WGT";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "weight_st" TO "WGT_ST";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "market_phase" TO "MKT_PHASE";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "risk_level" TO "RISK_LVL";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "regular_buy_sig" TO "REG_BUY_SIG";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "add_buy_amt" TO "ADD_BUY_AMT";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "rebuy_sig" TO "REBUY_SIG";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "final_action" TO "FINAL_ACT";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "reg_dt" TO "REG_DT";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "mod_dt" TO "MOD_DT";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "target_weight" TO "TGT_WGT";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "market_return_rt" TO "MKT_RTN_RT";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "risk_score" TO "RISK_SCORE";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "calc_reason" TO "CALC_RSN";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "purchase_amt" TO "PUR_AMT";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "profit_amt" TO "PFT_AMT";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "stock_return_rt" TO "STK_RTN_RT";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "weight_diff" TO "WGT_DIFF";
ALTER TABLE "TB_ANALYSIS" RENAME COLUMN "max_weight" TO "MAX_WGT";

ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "account_strategy_id" TO "ACCT_STRG_ID";
ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "briefing_id" TO "BRF_ID";
ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "account_type" TO "ACCT_TP";
ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "account_name" TO "ACCT_NM";
ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "market_signal" TO "MKT_SIG";
ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "regular_buy_signal" TO "REG_BUY_SIG";
ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "additional_buy_signal" TO "ADD_BUY_SIG";
ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "cash_strategy" TO "CASH_STRG";
ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "invest_amount" TO "INVEST_AMT";
ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "cash_balance" TO "CASH_BAL";
ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "cash_ratio" TO "CASH_RT";
ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "strategy_summary" TO "STRG_SUM";
ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "caution_message" TO "CAUTION_MSG";
ALTER TABLE "TB_BRF_ACCT_STRG" RENAME COLUMN "account_type_group" TO "ACCT_TP_GRP";

ALTER TABLE "TB_BRF_DTL" RENAME COLUMN "dtl_id" TO "DTL_ID";
ALTER TABLE "TB_BRF_DTL" RENAME COLUMN "brf_id" TO "BRF_ID";
ALTER TABLE "TB_BRF_DTL" RENAME COLUMN "item_cd" TO "ITEM_CD";
ALTER TABLE "TB_BRF_DTL" RENAME COLUMN "item_sum" TO "ITEM_SUM";
ALTER TABLE "TB_BRF_DTL" RENAME COLUMN "item_cont" TO "ITEM_CONT";
ALTER TABLE "TB_BRF_DTL" RENAME COLUMN "signal_cd" TO "SIG_CD";
ALTER TABLE "TB_BRF_DTL" RENAME COLUMN "act_yn" TO "ACT_YN";
ALTER TABLE "TB_BRF_DTL" RENAME COLUMN "reg_dt" TO "REG_DT";
ALTER TABLE "TB_BRF_DTL" RENAME COLUMN "mod_dt" TO "MOD_DT";

ALTER TABLE "TB_BRF_EVDC" RENAME COLUMN "evidence_id" TO "EVDC_ID";
ALTER TABLE "TB_BRF_EVDC" RENAME COLUMN "briefing_id" TO "BRF_ID";
ALTER TABLE "TB_BRF_EVDC" RENAME COLUMN "evidence_type_group" TO "EVDC_TP_GRP";
ALTER TABLE "TB_BRF_EVDC" RENAME COLUMN "evidence_type" TO "EVDC_TP";
ALTER TABLE "TB_BRF_EVDC" RENAME COLUMN "title" TO "TTL";
ALTER TABLE "TB_BRF_EVDC" RENAME COLUMN "source_name" TO "SRC_NM";
ALTER TABLE "TB_BRF_EVDC" RENAME COLUMN "source_url" TO "SRC_URL";
ALTER TABLE "TB_BRF_EVDC" RENAME COLUMN "published_at" TO "PUB_DT";
ALTER TABLE "TB_BRF_EVDC" RENAME COLUMN "summary" TO "SUM";
ALTER TABLE "TB_BRF_EVDC" RENAME COLUMN "impact_direction" TO "IMP_DIR";
ALTER TABLE "TB_BRF_EVDC" RENAME COLUMN "impact_level" TO "IMP_LVL";
ALTER TABLE "TB_BRF_EVDC" RENAME COLUMN "related_codes" TO "REL_CODES";

ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "item_cd" TO "ITEM_CD";
ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "item_nm" TO "ITEM_NM";
ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "sort_no" TO "SORT_NO";
ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "item_tp" TO "ITEM_TP";
ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "use_yn" TO "USE_YN";
ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "item_desc" TO "ITEM_DESC";
ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "brf_item_id" TO "BRF_ITEM_ID";
ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "brf_id" TO "BRF_ID";
ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "item_sum" TO "ITEM_SUM";
ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "item_cont" TO "ITEM_CONT";
ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "signal_cd" TO "SIG_CD";
ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "act_yn" TO "ACT_YN";
ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "reg_dt" TO "REG_DT";
ALTER TABLE "TB_BRF_ITEM" RENAME COLUMN "mod_dt" TO "MOD_DT";

ALTER TABLE "TB_BRF_MKT_IND" RENAME COLUMN "indicator_id" TO "IND_ID";
ALTER TABLE "TB_BRF_MKT_IND" RENAME COLUMN "briefing_id" TO "BRF_ID";
ALTER TABLE "TB_BRF_MKT_IND" RENAME COLUMN "market_code" TO "MKT_CD";
ALTER TABLE "TB_BRF_MKT_IND" RENAME COLUMN "close_price" TO "CLS_PRC";
ALTER TABLE "TB_BRF_MKT_IND" RENAME COLUMN "change_rate" TO "CHG_RT";
ALTER TABLE "TB_BRF_MKT_IND" RENAME COLUMN "foreign_net_amount" TO "FRGN_NET_AMT";
ALTER TABLE "TB_BRF_MKT_IND" RENAME COLUMN "institution_net_amount" TO "INST_NET_AMT";
ALTER TABLE "TB_BRF_MKT_IND" RENAME COLUMN "individual_net_amount" TO "INDV_NET_AMT";
ALTER TABLE "TB_BRF_MKT_IND" RENAME COLUMN "program_net_amount" TO "PRGM_NET_AMT";
ALTER TABLE "TB_BRF_MKT_IND" RENAME COLUMN "foreign_futures_amount" TO "FRGN_FUT_AMT";
ALTER TABLE "TB_BRF_MKT_IND" RENAME COLUMN "exchange_rate" TO "EXCH_RT";
ALTER TABLE "TB_BRF_MKT_IND" RENAME COLUMN "trading_value" TO "TRD_VAL";
ALTER TABLE "TB_BRF_MKT_IND" RENAME COLUMN "market_code_group" TO "MKT_CD_GRP";

ALTER TABLE "TB_BRF_STK" RENAME COLUMN "stk_sig_id" TO "STK_SIG_ID";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "brf_id" TO "BRF_ID";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "acct_tp" TO "ACCT_TP";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "stk_cd" TO "STK_CD";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "stk_nm" TO "STK_NM";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "light_cd" TO "LIGHT_CD";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "act_cd" TO "ACT_CD";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "buy_st" TO "BUY_ST";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "rebuy_cd" TO "REBUY_CD";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "rcm_amt" TO "RCM_AMT";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "rcm_rt" TO "RCM_RT";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "sig_rsn" TO "SIG_RSN";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "memo" TO "MEMO";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "reg_dt" TO "REG_DT";
ALTER TABLE "TB_BRF_STK" RENAME COLUMN "mod_dt" TO "MOD_DT";

ALTER TABLE "TB_BUY_SET" RENAME COLUMN "setting_id" TO "SET_ID";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "account_type" TO "ACCT_TP";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "stock_code" TO "STK_CD";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "stock_name" TO "STK_NM";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "cycle_type" TO "CYCLE_TP";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "day_of_week" TO "WEEK_DAY";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "day_of_month" TO "MONTH_DAY";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "amount" TO "AMT";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "quantity" TO "QTY";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "active_yn" TO "ACTV_YN";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "pause_reason" TO "PAUSE_RSN";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "start_date" TO "START_DT";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "end_date" TO "END_DT";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "created_at" TO "REG_DT";
ALTER TABLE "TB_BUY_SET" RENAME COLUMN "updated_at" TO "MOD_DT";

ALTER TABLE "TB_COM_CD" RENAME COLUMN "code_group" TO "CD_GRP";
ALTER TABLE "TB_COM_CD" RENAME COLUMN "code_key" TO "CD_KEY";
ALTER TABLE "TB_COM_CD" RENAME COLUMN "code_name" TO "CD_NM";
ALTER TABLE "TB_COM_CD" RENAME COLUMN "description" TO "DESC";
ALTER TABLE "TB_COM_CD" RENAME COLUMN "display_order" TO "DSP_ORD";
ALTER TABLE "TB_COM_CD" RENAME COLUMN "active_yn" TO "ACTV_YN";
ALTER TABLE "TB_COM_CD" RENAME COLUMN "created_at" TO "REG_DT";
ALTER TABLE "TB_COM_CD" RENAME COLUMN "updated_at" TO "MOD_DT";

ALTER TABLE "TB_HOLD" RENAME COLUMN "market_scope" TO "MKT_SCOPE";
ALTER TABLE "TB_HOLD" RENAME COLUMN "stock_code" TO "STK_CD";
ALTER TABLE "TB_HOLD" RENAME COLUMN "stock_name" TO "STK_NM";
ALTER TABLE "TB_HOLD" RENAME COLUMN "listing_scope" TO "LIST_SCOPE";
ALTER TABLE "TB_HOLD" RENAME COLUMN "asset_type" TO "ASSET_TP";
ALTER TABLE "TB_HOLD" RENAME COLUMN "exchange_name" TO "EXCH_NM";
ALTER TABLE "TB_HOLD" RENAME COLUMN "currency" TO "CURR";
ALTER TABLE "TB_HOLD" RENAME COLUMN "provider" TO "PRVDR";
ALTER TABLE "TB_HOLD" RENAME COLUMN "active_yn" TO "ACTV_YN";
ALTER TABLE "TB_HOLD" RENAME COLUMN "created_at" TO "REG_DT";
ALTER TABLE "TB_HOLD" RENAME COLUMN "updated_at" TO "MOD_DT";
ALTER TABLE "TB_HOLD" RENAME COLUMN "qty" TO "QTY";
ALTER TABLE "TB_HOLD" RENAME COLUMN "avg_prc" TO "AVG_PRC";

ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "trade_date" TO "TRD_DT";
ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "indicator_code" TO "IND_CD";
ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "indicator_name" TO "IND_NM";
ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "close_value" TO "CLS_VAL";
ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "change_value" TO "CHG_VAL";
ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "change_rate" TO "CHG_RT";
ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "source_name" TO "SRC_NM";
ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "collected_at" TO "CLCT_DT";
ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "open_value" TO "OPEN_VAL";
ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "high_value" TO "HIGH_VAL";
ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "low_value" TO "LOW_VAL";
ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "trading_volume" TO "TRD_VOL";
ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "trading_value" TO "TRD_VAL";
ALTER TABLE "TB_IDX_PRC" RENAME COLUMN "market_cap" TO "MKT_CAP";

ALTER TABLE "TB_INV_BRF" RENAME COLUMN "brf_id" TO "BRF_ID";
ALTER TABLE "TB_INV_BRF" RENAME COLUMN "brf_dt" TO "BRF_DT";
ALTER TABLE "TB_INV_BRF" RENAME COLUMN "brf_ttl" TO "BRF_TTL";
ALTER TABLE "TB_INV_BRF" RENAME COLUMN "brf_st" TO "BRF_ST";
ALTER TABLE "TB_INV_BRF" RENAME COLUMN "reg_dt" TO "REG_DT";
ALTER TABLE "TB_INV_BRF" RENAME COLUMN "mod_dt" TO "MOD_DT";

ALTER TABLE "TB_INV_DEC" RENAME COLUMN "id" TO "ID";
ALTER TABLE "TB_INV_DEC" RENAME COLUMN "decision_date" TO "DEC_DT";
ALTER TABLE "TB_INV_DEC" RENAME COLUMN "market_regime" TO "MKT_REGIME";
ALTER TABLE "TB_INV_DEC" RENAME COLUMN "market_score" TO "MKT_SCORE";
ALTER TABLE "TB_INV_DEC" RENAME COLUMN "sentiment_phase" TO "SENT_PHASE";
ALTER TABLE "TB_INV_DEC" RENAME COLUMN "sentiment_risk_score" TO "SENT_RISK_SCORE";
ALTER TABLE "TB_INV_DEC" RENAME COLUMN "total_minimum_buy_amount" TO "TOT_MIN_BUY_AMT";
ALTER TABLE "TB_INV_DEC" RENAME COLUMN "total_recommended_buy_amount" TO "TOT_RCM_BUY_AMT";
ALTER TABLE "TB_INV_DEC" RENAME COLUMN "newly_reserved_cash" TO "NEW_RSV_CASH";
ALTER TABLE "TB_INV_DEC" RENAME COLUMN "available_additional_buy_cash" TO "AVAIL_ADD_BUY_CASH";
ALTER TABLE "TB_INV_DEC" RENAME COLUMN "request_payload" TO "REQ_PAYLOAD";
ALTER TABLE "TB_INV_DEC" RENAME COLUMN "result_payload" TO "RSLT_PAYLOAD";
ALTER TABLE "TB_INV_DEC" RENAME COLUMN "created_at" TO "REG_DT";

ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "execution_id" TO "EXEC_ID";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "briefing_id" TO "BRF_ID";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "execution_date" TO "EXEC_DT";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "account_type_group" TO "ACCT_TP_GRP";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "account_type" TO "ACCT_TP";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "stock_code" TO "STK_CD";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "stock_name" TO "STK_NM";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "action_type" TO "ACT_TP";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "quantity" TO "QTY";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "price" TO "PRC";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "amount" TO "AMT";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "execution_status" TO "EXEC_ST";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "source_type_group" TO "SRC_TP_GRP";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "source_type" TO "SRC_TP";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "memo" TO "MEMO";
ALTER TABLE "TB_INV_EXEC" RENAME COLUMN "created_at" TO "REG_DT";

ALTER TABLE "TB_INV_STK_DEC" RENAME COLUMN "id" TO "ID";
ALTER TABLE "TB_INV_STK_DEC" RENAME COLUMN "investment_decision_id" TO "INV_DEC_ID";
ALTER TABLE "TB_INV_STK_DEC" RENAME COLUMN "account" TO "ACCT";
ALTER TABLE "TB_INV_STK_DEC" RENAME COLUMN "stock_code" TO "STK_CD";
ALTER TABLE "TB_INV_STK_DEC" RENAME COLUMN "stock_name" TO "STK_NM";
ALTER TABLE "TB_INV_STK_DEC" RENAME COLUMN "action_signal" TO "ACT_SIG";
ALTER TABLE "TB_INV_STK_DEC" RENAME COLUMN "multiplier" TO "MULT";
ALTER TABLE "TB_INV_STK_DEC" RENAME COLUMN "minimum_buy_amount" TO "MIN_BUY_AMT";
ALTER TABLE "TB_INV_STK_DEC" RENAME COLUMN "recommended_buy_amount" TO "RCM_BUY_AMT";
ALTER TABLE "TB_INV_STK_DEC" RENAME COLUMN "reserved_cash" TO "RSV_CASH";
ALTER TABLE "TB_INV_STK_DEC" RENAME COLUMN "reasons" TO "RSNS";
ALTER TABLE "TB_INV_STK_DEC" RENAME COLUMN "created_at" TO "REG_DT";

ALTER TABLE "TB_KRX_CLCT_JOB" RENAME COLUMN "id" TO "ID";
ALTER TABLE "TB_KRX_CLCT_JOB" RENAME COLUMN "base_date" TO "BASE_DT";
ALTER TABLE "TB_KRX_CLCT_JOB" RENAME COLUMN "status" TO "ST";
ALTER TABLE "TB_KRX_CLCT_JOB" RENAME COLUMN "total_count" TO "TOT_CNT";
ALTER TABLE "TB_KRX_CLCT_JOB" RENAME COLUMN "success_count" TO "SUCCESS_CNT";
ALTER TABLE "TB_KRX_CLCT_JOB" RENAME COLUMN "failed_count" TO "FAILED_CNT";
ALTER TABLE "TB_KRX_CLCT_JOB" RENAME COLUMN "created_at" TO "REG_DT";
ALTER TABLE "TB_KRX_CLCT_JOB" RENAME COLUMN "started_at" TO "START_DT";
ALTER TABLE "TB_KRX_CLCT_JOB" RENAME COLUMN "completed_at" TO "END_DT";

ALTER TABLE "TB_KRX_CLCT_JOB_ITEM" RENAME COLUMN "id" TO "ID";
ALTER TABLE "TB_KRX_CLCT_JOB_ITEM" RENAME COLUMN "job_id" TO "JOB_ID";
ALTER TABLE "TB_KRX_CLCT_JOB_ITEM" RENAME COLUMN "dataset_code" TO "DATA_CD";
ALTER TABLE "TB_KRX_CLCT_JOB_ITEM" RENAME COLUMN "status" TO "ST";
ALTER TABLE "TB_KRX_CLCT_JOB_ITEM" RENAME COLUMN "received_count" TO "RCV_CNT";
ALTER TABLE "TB_KRX_CLCT_JOB_ITEM" RENAME COLUMN "stored_count" TO "STRD_CNT";
ALTER TABLE "TB_KRX_CLCT_JOB_ITEM" RENAME COLUMN "error_message" TO "ERR_MSG";
ALTER TABLE "TB_KRX_CLCT_JOB_ITEM" RENAME COLUMN "started_at" TO "START_DT";
ALTER TABLE "TB_KRX_CLCT_JOB_ITEM" RENAME COLUMN "completed_at" TO "END_DT";

ALTER TABLE "TB_KRX_DATA_ROW" RENAME COLUMN "id" TO "ID";
ALTER TABLE "TB_KRX_DATA_ROW" RENAME COLUMN "dataset_code" TO "DATA_CD";
ALTER TABLE "TB_KRX_DATA_ROW" RENAME COLUMN "base_date" TO "BASE_DT";
ALTER TABLE "TB_KRX_DATA_ROW" RENAME COLUMN "row_key" TO "ROW_KEY";
ALTER TABLE "TB_KRX_DATA_ROW" RENAME COLUMN "payload" TO "PAYLOAD";
ALTER TABLE "TB_KRX_DATA_ROW" RENAME COLUMN "created_at" TO "REG_DT";
ALTER TABLE "TB_KRX_DATA_ROW" RENAME COLUMN "updated_at" TO "MOD_DT";

ALTER TABLE "TB_MKT_CAL" RENAME COLUMN "cal_dt" TO "CAL_DT";
ALTER TABLE "TB_MKT_CAL" RENAME COLUMN "market_cd" TO "MKT_CD";
ALTER TABLE "TB_MKT_CAL" RENAME COLUMN "open_yn" TO "OPEN_YN";
ALTER TABLE "TB_MKT_CAL" RENAME COLUMN "holiday_nm" TO "HLDY_NM";
ALTER TABLE "TB_MKT_CAL" RENAME COLUMN "reg_dt" TO "REG_DT";
ALTER TABLE "TB_MKT_CAL" RENAME COLUMN "mod_dt" TO "MOD_DT";

ALTER TABLE "TB_RISK_RULE" RENAME COLUMN "rule_id" TO "RULE_ID";
ALTER TABLE "TB_RISK_RULE" RENAME COLUMN "rule_nm" TO "RULE_NM";
ALTER TABLE "TB_RISK_RULE" RENAME COLUMN "min_drop_rt" TO "MIN_DROP_RT";
ALTER TABLE "TB_RISK_RULE" RENAME COLUMN "max_drop_rt" TO "MAX_DROP_RT";
ALTER TABLE "TB_RISK_RULE" RENAME COLUMN "risk_score" TO "RISK_SCORE";
ALTER TABLE "TB_RISK_RULE" RENAME COLUMN "cash_input_rt" TO "CASH_IN_RT";
ALTER TABLE "TB_RISK_RULE" RENAME COLUMN "use_yn" TO "USE_YN";
ALTER TABLE "TB_RISK_RULE" RENAME COLUMN "reg_dt" TO "REG_DT";
ALTER TABLE "TB_RISK_RULE" RENAME COLUMN "mod_dt" TO "MOD_DT";

ALTER TABLE "TB_STK_PRC" RENAME COLUMN "id" TO "ID";
ALTER TABLE "TB_STK_PRC" RENAME COLUMN "symbol" TO "SYM";
ALTER TABLE "TB_STK_PRC" RENAME COLUMN "trading_day" TO "TRD_DT";
ALTER TABLE "TB_STK_PRC" RENAME COLUMN "open_price" TO "OPEN_PRC";
ALTER TABLE "TB_STK_PRC" RENAME COLUMN "high_price" TO "HIGH_PRC";
ALTER TABLE "TB_STK_PRC" RENAME COLUMN "low_price" TO "LOW_PRC";
ALTER TABLE "TB_STK_PRC" RENAME COLUMN "close_price" TO "CLS_PRC";
ALTER TABLE "TB_STK_PRC" RENAME COLUMN "adjusted_close" TO "ADJ_CLS";
ALTER TABLE "TB_STK_PRC" RENAME COLUMN "volume" TO "VOL";
ALTER TABLE "TB_STK_PRC" RENAME COLUMN "provider" TO "PRVDR";
ALTER TABLE "TB_STK_PRC" RENAME COLUMN "created_at" TO "REG_DT";
ALTER TABLE "TB_STK_PRC" RENAME COLUMN "updated_at" TO "MOD_DT";
ALTER TABLE "TB_STK_PRC" RENAME COLUMN "market_scope" TO "MKT_SCOPE";

ALTER TABLE "TB_STK_SET" RENAME COLUMN "acct_tp" TO "ACCT_TP";
ALTER TABLE "TB_STK_SET" RENAME COLUMN "stk_cd" TO "STK_CD";
ALTER TABLE "TB_STK_SET" RENAME COLUMN "stk_grade" TO "STK_GRD";
ALTER TABLE "TB_STK_SET" RENAME COLUMN "target_weight" TO "TGT_WGT";
ALTER TABLE "TB_STK_SET" RENAME COLUMN "benchmark_cd" TO "BM_CD";
ALTER TABLE "TB_STK_SET" RENAME COLUMN "reg_dt" TO "REG_DT";
ALTER TABLE "TB_STK_SET" RENAME COLUMN "mod_dt" TO "MOD_DT";

CREATE OR REPLACE VIEW tb_acct AS
SELECT
    "ACCT_TP" AS "acct_tp",
    "ACCT_NM" AS "acct_nm",
    "TOT_AMT" AS "total_amt",
    "AVAIL_CASH" AS "avail_cash",
    "CURR" AS "currency",
    "USE_YN" AS "use_yn",
    "REG_DT" AS "reg_dt",
    "MOD_DT" AS "mod_dt"
FROM "TB_ACCT";

CREATE OR REPLACE VIEW tb_analysis AS
SELECT
    "ANLYS_ID" AS "analysis_id",
    "ANLYS_DT" AS "analysis_dt",
    "ACCT_TP" AS "acct_tp",
    "STK_CD" AS "stk_cd",
    "MKT_VAL" AS "market_value",
    "CUR_WGT" AS "current_weight",
    "WGT_ST" AS "weight_st",
    "MKT_PHASE" AS "market_phase",
    "RISK_LVL" AS "risk_level",
    "REG_BUY_SIG" AS "regular_buy_sig",
    "ADD_BUY_AMT" AS "add_buy_amt",
    "REBUY_SIG" AS "rebuy_sig",
    "FINAL_ACT" AS "final_action",
    "REG_DT" AS "reg_dt",
    "MOD_DT" AS "mod_dt",
    "TGT_WGT" AS "target_weight",
    "MKT_RTN_RT" AS "market_return_rt",
    "RISK_SCORE" AS "risk_score",
    "CALC_RSN" AS "calc_reason",
    "PUR_AMT" AS "purchase_amt",
    "PFT_AMT" AS "profit_amt",
    "STK_RTN_RT" AS "stock_return_rt",
    "WGT_DIFF" AS "weight_diff",
    "MAX_WGT" AS "max_weight"
FROM "TB_ANALYSIS";

CREATE OR REPLACE VIEW tb_brf_acct_strg AS
SELECT
    "ACCT_STRG_ID" AS "account_strategy_id",
    "BRF_ID" AS "briefing_id",
    "ACCT_TP" AS "account_type",
    "ACCT_NM" AS "account_name",
    "MKT_SIG" AS "market_signal",
    "REG_BUY_SIG" AS "regular_buy_signal",
    "ADD_BUY_SIG" AS "additional_buy_signal",
    "CASH_STRG" AS "cash_strategy",
    "INVEST_AMT" AS "invest_amount",
    "CASH_BAL" AS "cash_balance",
    "CASH_RT" AS "cash_ratio",
    "STRG_SUM" AS "strategy_summary",
    "CAUTION_MSG" AS "caution_message",
    "ACCT_TP_GRP" AS "account_type_group"
FROM "TB_BRF_ACCT_STRG";

CREATE OR REPLACE VIEW tb_brf_dtl AS
SELECT
    "DTL_ID" AS "dtl_id",
    "BRF_ID" AS "brf_id",
    "ITEM_CD" AS "item_cd",
    "ITEM_SUM" AS "item_sum",
    "ITEM_CONT" AS "item_cont",
    "SIG_CD" AS "signal_cd",
    "ACT_YN" AS "act_yn",
    "REG_DT" AS "reg_dt",
    "MOD_DT" AS "mod_dt"
FROM "TB_BRF_DTL";

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
    "REL_CODES" AS "related_codes"
FROM "TB_BRF_EVDC";

CREATE OR REPLACE VIEW tb_brf_item AS
SELECT
    "ITEM_CD" AS "item_cd",
    "ITEM_NM" AS "item_nm",
    "SORT_NO" AS "sort_no",
    "ITEM_TP" AS "item_tp",
    "USE_YN" AS "use_yn",
    "ITEM_DESC" AS "item_desc",
    "BRF_ITEM_ID" AS "brf_item_id",
    "BRF_ID" AS "brf_id",
    "ITEM_SUM" AS "item_sum",
    "ITEM_CONT" AS "item_cont",
    "SIG_CD" AS "signal_cd",
    "ACT_YN" AS "act_yn",
    "REG_DT" AS "reg_dt",
    "MOD_DT" AS "mod_dt"
FROM "TB_BRF_ITEM";

CREATE OR REPLACE VIEW tb_brf_mkt_ind AS
SELECT
    "IND_ID" AS "indicator_id",
    "BRF_ID" AS "briefing_id",
    "MKT_CD" AS "market_code",
    "CLS_PRC" AS "close_price",
    "CHG_RT" AS "change_rate",
    "FRGN_NET_AMT" AS "foreign_net_amount",
    "INST_NET_AMT" AS "institution_net_amount",
    "INDV_NET_AMT" AS "individual_net_amount",
    "PRGM_NET_AMT" AS "program_net_amount",
    "FRGN_FUT_AMT" AS "foreign_futures_amount",
    "EXCH_RT" AS "exchange_rate",
    "TRD_VAL" AS "trading_value",
    "MKT_CD_GRP" AS "market_code_group"
FROM "TB_BRF_MKT_IND";

CREATE OR REPLACE VIEW tb_brf_stk AS
SELECT
    "STK_SIG_ID" AS "stk_sig_id",
    "BRF_ID" AS "brf_id",
    "ACCT_TP" AS "acct_tp",
    "STK_CD" AS "stk_cd",
    "STK_NM" AS "stk_nm",
    "LIGHT_CD" AS "light_cd",
    "ACT_CD" AS "act_cd",
    "BUY_ST" AS "buy_st",
    "REBUY_CD" AS "rebuy_cd",
    "RCM_AMT" AS "rcm_amt",
    "RCM_RT" AS "rcm_rt",
    "SIG_RSN" AS "sig_rsn",
    "MEMO" AS "memo",
    "REG_DT" AS "reg_dt",
    "MOD_DT" AS "mod_dt"
FROM "TB_BRF_STK";

CREATE OR REPLACE VIEW tb_buy_set AS
SELECT
    "SET_ID" AS "setting_id",
    "ACCT_TP" AS "account_type",
    "STK_CD" AS "stock_code",
    "STK_NM" AS "stock_name",
    "CYCLE_TP" AS "cycle_type",
    "WEEK_DAY" AS "day_of_week",
    "MONTH_DAY" AS "day_of_month",
    "AMT" AS "amount",
    "QTY" AS "quantity",
    "ACTV_YN" AS "active_yn",
    "PAUSE_RSN" AS "pause_reason",
    "START_DT" AS "start_date",
    "END_DT" AS "end_date",
    "REG_DT" AS "created_at",
    "MOD_DT" AS "updated_at"
FROM "TB_BUY_SET";

CREATE OR REPLACE VIEW tb_com_cd AS
SELECT
    "CD_GRP" AS "code_group",
    "CD_KEY" AS "code_key",
    "CD_NM" AS "code_name",
    "DESC" AS "description",
    "DSP_ORD" AS "display_order",
    "ACTV_YN" AS "active_yn",
    "REG_DT" AS "created_at",
    "MOD_DT" AS "updated_at"
FROM "TB_COM_CD";

CREATE OR REPLACE VIEW tb_hold AS
SELECT
    "MKT_SCOPE" AS "market_scope",
    "STK_CD" AS "stock_code",
    "STK_NM" AS "stock_name",
    "LIST_SCOPE" AS "listing_scope",
    "ASSET_TP" AS "asset_type",
    "EXCH_NM" AS "exchange_name",
    "CURR" AS "currency",
    "PRVDR" AS "provider",
    "ACTV_YN" AS "active_yn",
    "REG_DT" AS "created_at",
    "MOD_DT" AS "updated_at",
    "QTY" AS "qty",
    "AVG_PRC" AS "avg_prc"
FROM "TB_HOLD";

CREATE OR REPLACE VIEW tb_idx_prc AS
SELECT
    "TRD_DT" AS "trade_date",
    "IND_CD" AS "indicator_code",
    "IND_NM" AS "indicator_name",
    "CLS_VAL" AS "close_value",
    "CHG_VAL" AS "change_value",
    "CHG_RT" AS "change_rate",
    "SRC_NM" AS "source_name",
    "CLCT_DT" AS "collected_at",
    "OPEN_VAL" AS "open_value",
    "HIGH_VAL" AS "high_value",
    "LOW_VAL" AS "low_value",
    "TRD_VOL" AS "trading_volume",
    "TRD_VAL" AS "trading_value",
    "MKT_CAP" AS "market_cap"
FROM "TB_IDX_PRC";

CREATE OR REPLACE VIEW tb_inv_brf AS
SELECT
    "BRF_ID" AS "brf_id",
    "BRF_DT" AS "brf_dt",
    "BRF_TTL" AS "brf_ttl",
    "BRF_ST" AS "brf_st",
    "REG_DT" AS "reg_dt",
    "MOD_DT" AS "mod_dt"
FROM "TB_INV_BRF";

CREATE OR REPLACE VIEW tb_inv_dec AS
SELECT
    "ID" AS "id",
    "DEC_DT" AS "decision_date",
    "MKT_REGIME" AS "market_regime",
    "MKT_SCORE" AS "market_score",
    "SENT_PHASE" AS "sentiment_phase",
    "SENT_RISK_SCORE" AS "sentiment_risk_score",
    "TOT_MIN_BUY_AMT" AS "total_minimum_buy_amount",
    "TOT_RCM_BUY_AMT" AS "total_recommended_buy_amount",
    "NEW_RSV_CASH" AS "newly_reserved_cash",
    "AVAIL_ADD_BUY_CASH" AS "available_additional_buy_cash",
    "REQ_PAYLOAD" AS "request_payload",
    "RSLT_PAYLOAD" AS "result_payload",
    "REG_DT" AS "created_at"
FROM "TB_INV_DEC";

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
    "REG_DT" AS "created_at"
FROM "TB_INV_EXEC";

CREATE OR REPLACE VIEW tb_inv_stk_dec AS
SELECT
    "ID" AS "id",
    "INV_DEC_ID" AS "investment_decision_id",
    "ACCT" AS "account",
    "STK_CD" AS "stock_code",
    "STK_NM" AS "stock_name",
    "ACT_SIG" AS "action_signal",
    "MULT" AS "multiplier",
    "MIN_BUY_AMT" AS "minimum_buy_amount",
    "RCM_BUY_AMT" AS "recommended_buy_amount",
    "RSV_CASH" AS "reserved_cash",
    "RSNS" AS "reasons",
    "REG_DT" AS "created_at"
FROM "TB_INV_STK_DEC";

CREATE OR REPLACE VIEW tb_krx_clct_job AS
SELECT
    "ID" AS "id",
    "BASE_DT" AS "base_date",
    "ST" AS "status",
    "TOT_CNT" AS "total_count",
    "SUCCESS_CNT" AS "success_count",
    "FAILED_CNT" AS "failed_count",
    "REG_DT" AS "created_at",
    "START_DT" AS "started_at",
    "END_DT" AS "completed_at"
FROM "TB_KRX_CLCT_JOB";

CREATE OR REPLACE VIEW tb_krx_clct_job_item AS
SELECT
    "ID" AS "id",
    "JOB_ID" AS "job_id",
    "DATA_CD" AS "dataset_code",
    "ST" AS "status",
    "RCV_CNT" AS "received_count",
    "STRD_CNT" AS "stored_count",
    "ERR_MSG" AS "error_message",
    "START_DT" AS "started_at",
    "END_DT" AS "completed_at"
FROM "TB_KRX_CLCT_JOB_ITEM";

CREATE OR REPLACE VIEW tb_krx_data_row AS
SELECT
    "ID" AS "id",
    "DATA_CD" AS "dataset_code",
    "BASE_DT" AS "base_date",
    "ROW_KEY" AS "row_key",
    "PAYLOAD" AS "payload",
    "REG_DT" AS "created_at",
    "MOD_DT" AS "updated_at"
FROM "TB_KRX_DATA_ROW";

CREATE OR REPLACE VIEW tb_mkt_cal AS
SELECT
    "CAL_DT" AS "cal_dt",
    "MKT_CD" AS "market_cd",
    "OPEN_YN" AS "open_yn",
    "HLDY_NM" AS "holiday_nm",
    "REG_DT" AS "reg_dt",
    "MOD_DT" AS "mod_dt"
FROM "TB_MKT_CAL";

CREATE OR REPLACE VIEW tb_risk_rule AS
SELECT
    "RULE_ID" AS "rule_id",
    "RULE_NM" AS "rule_nm",
    "MIN_DROP_RT" AS "min_drop_rt",
    "MAX_DROP_RT" AS "max_drop_rt",
    "RISK_SCORE" AS "risk_score",
    "CASH_IN_RT" AS "cash_input_rt",
    "USE_YN" AS "use_yn",
    "REG_DT" AS "reg_dt",
    "MOD_DT" AS "mod_dt"
FROM "TB_RISK_RULE";

CREATE OR REPLACE VIEW tb_stk_prc AS
SELECT
    "ID" AS "id",
    "SYM" AS "symbol",
    "TRD_DT" AS "trading_day",
    "OPEN_PRC" AS "open_price",
    "HIGH_PRC" AS "high_price",
    "LOW_PRC" AS "low_price",
    "CLS_PRC" AS "close_price",
    "ADJ_CLS" AS "adjusted_close",
    "VOL" AS "volume",
    "PRVDR" AS "provider",
    "REG_DT" AS "created_at",
    "MOD_DT" AS "updated_at",
    "MKT_SCOPE" AS "market_scope"
FROM "TB_STK_PRC";

CREATE OR REPLACE VIEW tb_stk_set AS
SELECT
    "ACCT_TP" AS "acct_tp",
    "STK_CD" AS "stk_cd",
    "STK_GRD" AS "stk_grade",
    "TGT_WGT" AS "target_weight",
    "BM_CD" AS "benchmark_cd",
    "REG_DT" AS "reg_dt",
    "MOD_DT" AS "mod_dt"
FROM "TB_STK_SET";

