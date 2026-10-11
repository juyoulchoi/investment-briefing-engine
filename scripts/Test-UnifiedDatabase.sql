\set ON_ERROR_STOP on
BEGIN;
DO $$ DECLARE id bigint; q numeric; w numeric; original_w numeric; BEGIN
 IF current_database() NOT LIKE 'investment_unified_%' THEN RAISE EXCEPTION 'Candidate database required'; END IF;
 SELECT "HOLD_ID","HOLD_QTY" INTO id,q FROM portfolio."TB_HOLD" ORDER BY "HOLD_ID" LIMIT 1;
 SELECT "TGT_WGT" INTO original_w FROM v2."TB_HOLD" WHERE "HOLD_ID"=id;
 UPDATE v1."TB_HOLD" SET "TGT_WGT"=12.3456 WHERE "HOLD_ID"=id;
 SELECT "TGT_WGT" INTO w FROM v2."TB_HOLD" WHERE "HOLD_ID"=id;
 IF w IS DISTINCT FROM original_w THEN RAISE EXCEPTION 'V1 model write leaked to V2'; END IF;
 UPDATE v1."TB_HOLD" SET "HOLD_QTY"=q+1 WHERE "HOLD_ID"=id;
 IF (SELECT "HOLD_QTY" FROM v2."TB_HOLD" WHERE "HOLD_ID"=id) <> q+1 THEN RAISE EXCEPTION 'Portfolio input is not shared'; END IF;
 BEGIN
  UPDATE v2."TB_HOLD" SET "HOLD_QTY"=-1 WHERE "HOLD_ID"=id;
  RAISE EXCEPTION 'Negative holding accepted';
 EXCEPTION WHEN check_violation THEN NULL;
 END;
 IF (SELECT count(*) FROM v1."TB_HOLD") <> (SELECT count(*) FROM v2."TB_HOLD") THEN RAISE EXCEPTION 'Hold counts differ'; END IF;
 RAISE NOTICE 'Shared portfolio visibility, model isolation and core constraints pass';
END $$;
DO $$ DECLARE a text; s text; before_value numeric; BEGIN
 SELECT "ACCT_TP","STK_CD" INTO a,s FROM portfolio."TB_REG_BUY" LIMIT 1;
 SELECT "RCMD_BUY_AMT" INTO before_value FROM v2."TB_REG_BUY" WHERE "ACCT_TP"=a AND "STK_CD"=s;
 UPDATE v1."TB_REG_BUY" SET "RCMD_BUY_AMT"=12345 WHERE "ACCT_TP"=a AND "STK_CD"=s;
 IF (SELECT "RCMD_BUY_AMT" FROM v2."TB_REG_BUY" WHERE "ACCT_TP"=a AND "STK_CD"=s) IS DISTINCT FROM before_value THEN RAISE EXCEPTION 'Recommendation write leaked'; END IF;
 RAISE NOTICE 'Recommendation isolation passes';
END $$;
SELECT 'v1' AS model,count(*) FROM v1."TB_HOLD" UNION ALL SELECT 'v2',count(*) FROM v2."TB_HOLD";
SELECT 'stock_id_mappings' AS check,count(*) FROM migration.stock_id_map WHERE old_id<>new_id;
SELECT 'unvalidated_fks' AS check,count(*) FROM pg_constraint WHERE contype='f' AND NOT convalidated;
ROLLBACK;
