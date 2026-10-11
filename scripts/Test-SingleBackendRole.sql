\set ON_ERROR_STOP on
BEGIN;
SET LOCAL ROLE unified_backend;
SET LOCAL search_path=v1,portfolio,source;
DO $$ DECLARE id bigint; qty numeric; BEGIN
 IF current_schema() <> 'v1' THEN RAISE EXCEPTION 'Expected V1 routing'; END IF;
 SELECT "HOLD_ID","HOLD_QTY" INTO id,qty FROM "TB_HOLD" LIMIT 1;
 UPDATE "TB_HOLD" SET "HOLD_QTY"=qty WHERE "HOLD_ID"=id;
 UPDATE source."TB_EXCH_DAY" SET "EXCH_RT"="EXCH_RT" WHERE false;
 IF NOT has_schema_privilege(current_user,'source','USAGE') THEN RAISE EXCEPTION 'Missing source access'; END IF;
 IF has_schema_privilege(current_user,'v2','USAGE') OR has_schema_privilege(current_user,'archive_v2','USAGE') THEN
  RAISE EXCEPTION 'Retired V2 must remain inaccessible to runtime role';
 END IF;
 IF has_schema_privilege(current_user,'source','CREATE') THEN RAISE EXCEPTION 'Runtime role can perform DDL'; END IF;
 RAISE NOTICE 'Single backend portfolio/source writes and retired model isolation pass';
END $$;
ROLLBACK;
