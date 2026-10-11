\set ON_ERROR_STOP on
BEGIN;
DO $$ BEGIN
 IF current_database() NOT LIKE 'investment_unified_%' THEN RAISE EXCEPTION 'Unified database required'; END IF;
 IF to_regclass('source."TB_DATA_CORR_HIS"') IS NULL THEN
  ALTER TABLE v1."TB_DATA_CORR_HIS" SET SCHEMA source;
  ALTER TABLE source."TB_DATA_CORR_HIS" ADD COLUMN "SOURCE_ORIGIN" varchar(30) NOT NULL DEFAULT 'V1_LEGACY';
  ALTER TABLE source."TB_DATA_CORR_HIS" ADD COLUMN "ORIGINAL_SOURCE_KEY" text;
  UPDATE source."TB_DATA_CORR_HIS" SET "ORIGINAL_SOURCE_KEY"="SOURCE_KEY";
  ALTER TABLE v2."TB_DATA_CORR_HIS" SET SCHEMA archive_v2;
 END IF;
END $$;
CREATE TABLE IF NOT EXISTS migration.correction_id_map(old_id bigint PRIMARY KEY,new_id bigint NOT NULL UNIQUE);
DO $$ DECLARE r record; new_id bigint; BEGIN
 FOR r IN SELECT * FROM archive_v2."TB_DATA_CORR_HIS" h
          WHERE NOT EXISTS (SELECT 1 FROM migration.correction_id_map m WHERE m.old_id=h."CORR_ID") ORDER BY "CORR_ID" LOOP
  INSERT INTO source."TB_DATA_CORR_HIS"("PROVIDER_CD","DATASET_CD","SOURCE_KEY","BASE_DT","CHANGED_COLS",
     "BEFORE_PAYLOAD","AFTER_PAYLOAD","PAYLOAD_VER","RECOLLECT_JOB_ID","CORR_REASON","DETECTED_DTTM","SOURCE_ORIGIN","ORIGINAL_SOURCE_KEY")
  VALUES(r."PROVIDER_CD",r."DATASET_CD",'V2_LEGACY:'||r."SOURCE_KEY",r."BASE_DT",r."CHANGED_COLS",
     r."BEFORE_PAYLOAD",r."AFTER_PAYLOAD",r."PAYLOAD_VER",r."RECOLLECT_JOB_ID",r."CORR_REASON",r."DETECTED_DTTM",'V2_LEGACY',r."SOURCE_KEY")
  RETURNING "CORR_ID" INTO new_id;
  INSERT INTO migration.correction_id_map VALUES(r."CORR_ID",new_id);
 END LOOP;
END $$;
DO $$ DECLARE r record; BEGIN
 IF EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='v2."TB_RECALC_QUEUE"'::regclass
       AND confrelid='archive_v2."TB_DATA_CORR_HIS"'::regclass) THEN
  FOR r IN SELECT conname FROM pg_constraint WHERE conrelid='v2."TB_RECALC_QUEUE"'::regclass
        AND confrelid='archive_v2."TB_DATA_CORR_HIS"'::regclass LOOP
   EXECUTE format('ALTER TABLE v2."TB_RECALC_QUEUE" DROP CONSTRAINT %I',r.conname);
  END LOOP;
  UPDATE v2."TB_RECALC_QUEUE" q SET "CORR_ID"=-m.new_id FROM migration.correction_id_map m WHERE q."CORR_ID"=m.old_id;
  UPDATE v2."TB_RECALC_QUEUE" SET "CORR_ID"=-"CORR_ID" WHERE "CORR_ID"<0;
  ALTER TABLE v2."TB_RECALC_QUEUE" ADD CONSTRAINT "FK_UNIFIED_CORRECTION" FOREIGN KEY("CORR_ID") REFERENCES source."TB_DATA_CORR_HIS"("CORR_ID");
 END IF;
 IF to_regprocedure('v1."FN_AUDIT_PROVIDER_CORRECTION"()') IS NOT NULL THEN
  ALTER FUNCTION v1."FN_AUDIT_PROVIDER_CORRECTION"() SET SCHEMA source;
 END IF;
END $$;

CREATE OR REPLACE FUNCTION source."FN_AUDIT_PROVIDER_CORRECTION"() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,source AS $$
DECLARE old_payload jsonb:=to_jsonb(OLD);new_payload jsonb:=to_jsonb(NEW);
 changes jsonb;correction_id bigint;source_date date;source_key text;next_version integer;
BEGIN
 SELECT jsonb_object_agg(key,jsonb_build_object('before',old_payload->key,'after',new_payload->key)) INTO changes
 FROM (SELECT key FROM jsonb_object_keys(old_payload) key UNION SELECT key FROM jsonb_object_keys(new_payload) key) keys
 WHERE old_payload->key IS DISTINCT FROM new_payload->key
   AND key NOT IN ('COLLECT_DTTM','UPD_DTTM','MOD_DT','REG_DT','LAST_COLLECT_DTTM');
 IF changes IS NULL OR changes='{}'::jsonb THEN RETURN NEW; END IF;
 source_date:=COALESCE(NULLIF(new_payload->>'BASE_DT','')::date,NULLIF(new_payload->>'TRADE_DT','')::date,NULLIF(new_payload->>'OBS_DT','')::date);
 source_key:=TG_TABLE_NAME||':'||COALESCE(source_date::text,'NO_DATE')||':'||concat_ws('|',
  new_payload->>'STK_ID',new_payload->>'STK_CD',new_payload->>'IDX_CD',new_payload->>'BOND_CD',new_payload->>'SERIES_ID',
  new_payload->>'ISU_CD',new_payload->>'DATASET_CD',new_payload->>'BASE_CURR_CD',new_payload->>'QUOTE_CURR_CD');
 SELECT COALESCE(max("PAYLOAD_VER"),0)+1 INTO next_version FROM source."TB_DATA_CORR_HIS"
 WHERE "PROVIDER_CD"=TG_ARGV[0] AND "DATASET_CD"=TG_ARGV[1] AND "SOURCE_KEY"=source_key;
 INSERT INTO source."TB_DATA_CORR_HIS"("PROVIDER_CD","DATASET_CD","SOURCE_KEY","BASE_DT","CHANGED_COLS","BEFORE_PAYLOAD",
   "AFTER_PAYLOAD","PAYLOAD_VER","CORR_REASON","SOURCE_ORIGIN","ORIGINAL_SOURCE_KEY")
 VALUES(TG_ARGV[0],TG_ARGV[1],source_key,source_date,changes,old_payload,new_payload,next_version,
   'PROVIDER_RECOLLECTION_VALUE_CHANGED','UNIFIED',source_key) RETURNING "CORR_ID" INTO correction_id;
 INSERT INTO v1."TB_RECALC_QUEUE"("CORR_ID","TARGET_TP","BASE_DT") VALUES
  (correction_id,'MARKET_SNAPSHOT',source_date),(correction_id,'MARKET_SENTIMENT',source_date),
  (correction_id,'INVESTMENT_DECISION',source_date),(correction_id,'BRIEFING',source_date);
 INSERT INTO v2."TB_RECALC_QUEUE"("CORR_ID","TARGET_TP","BASE_DT") VALUES
  (correction_id,'MARKET_SNAPSHOT',source_date),(correction_id,'MARKET_SENTIMENT',source_date),
  (correction_id,'INVESTMENT_DECISION',source_date),(correction_id,'BRIEFING',source_date);
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS "TRG_UNIFIED_EXCHANGE_CORRECTION" ON source."TB_EXCH_DAY";
CREATE TRIGGER "TRG_UNIFIED_EXCHANGE_CORRECTION" AFTER UPDATE OF "EXCH_RT" ON source."TB_EXCH_DAY"
FOR EACH ROW WHEN (OLD."EXCH_RT" IS DISTINCT FROM NEW."EXCH_RT")
EXECUTE FUNCTION source."FN_AUDIT_PROVIDER_CORRECTION"('YAHOO','EXCHANGE_DAY');
COMMIT;
