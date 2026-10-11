\set ON_ERROR_STOP on
BEGIN;
SET ROLE unified_v1;
SET search_path=v1,portfolio,source;
SELECT count(*) AS v1_shared_holdings FROM "TB_HOLD";
DO $$ BEGIN
 BEGIN
  UPDATE source."TB_PRC_DAY" SET "CLS_PRC"="CLS_PRC" WHERE false;
  RAISE EXCEPTION 'V1 can write provider prices';
 EXCEPTION WHEN insufficient_privilege THEN NULL; END;
 BEGIN
  UPDATE v2."TB_HOLD_MODEL" SET "TGT_WGT"="TGT_WGT" WHERE false;
  RAISE EXCEPTION 'V1 can write V2 model results';
 EXCEPTION WHEN insufficient_privilege THEN NULL; END;
END $$;
RESET ROLE;
SET ROLE unified_v2;
SET search_path=v2,portfolio,source;
SELECT count(*) AS v2_shared_holdings FROM "TB_HOLD";
UPDATE "TB_HOLD" SET "TGT_WGT"="TGT_WGT" WHERE "HOLD_ID"=(SELECT min("HOLD_ID") FROM "TB_HOLD");
RESET ROLE;
SET ROLE unified_collector;
SET search_path=source,portfolio;
SELECT count(*) AS collector_holdings FROM "TB_HOLD";
DO $$ BEGIN
 BEGIN
  UPDATE portfolio."TB_HOLD" SET "HOLD_QTY"="HOLD_QTY" WHERE false;
  RAISE EXCEPTION 'Collector can modify actual quantity';
 EXCEPTION WHEN insufficient_privilege THEN NULL; END;
 BEGIN
  DELETE FROM v1."TB_INV_DEC" WHERE false;
  RAISE EXCEPTION 'Collector can modify model decisions';
 EXCEPTION WHEN insufficient_privilege THEN NULL; END;
END $$;
UPDATE portfolio."TB_HOLD" SET "CUR_PRC"="CUR_PRC" WHERE false;
RESET ROLE;
ROLLBACK;
