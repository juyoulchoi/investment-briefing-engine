\set ON_ERROR_STOP on
BEGIN;
-- This migration only accepts the isolated rehearsal database.
DO $$ BEGIN
  IF current_database() NOT LIKE 'investment_unified_%' THEN RAISE EXCEPTION 'Candidate database required'; END IF;
  IF to_regclass('portfolio."TB_HOLD"') IS NOT NULL THEN RAISE EXCEPTION 'Already unified'; END IF;
END $$;
CREATE SCHEMA archive_v1;
CREATE SCHEMA archive_v2;
CREATE SCHEMA migration;
CREATE TABLE migration.stock_id_map AS
SELECT b."STK_ID" AS old_id,a."STK_ID" AS new_id,b."MKT_CD",b."STK_CD"
FROM v2."TB_STK" b JOIN v1."TB_STK" a USING ("MKT_CD","STK_CD");
DO $$ BEGIN
 IF (SELECT count(*) FROM migration.stock_id_map) <> (SELECT count(*) FROM v2."TB_STK") THEN
  RAISE EXCEPTION 'Unmapped V2 stock';
 END IF;
END $$;
-- Remember every V2 FK before moving the historical copies. Rebind only those
-- whose referenced relation becomes a shared physical table.
CREATE TABLE migration.foreign_keys AS
SELECT k.oid,k.conname,c.relname AS table_name,rc.relname AS referenced_table,
       k.conkey,k.confkey,pg_get_constraintdef(k.oid) AS definition
FROM pg_constraint k JOIN pg_class c ON c.oid=k.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace
JOIN pg_class rc ON rc.oid=k.confrelid
WHERE k.contype='f' AND n.nspname='v2';

CREATE TABLE migration.table_routes(table_name text PRIMARY KEY,target_schema text NOT NULL);
INSERT INTO migration.table_routes VALUES
('TB_ACCT','portfolio'),('TB_HOLD','portfolio'),('TB_REG_BUY','portfolio'),
('TB_CASH_HIS','portfolio'),('TB_CASH_RSV','portfolio'),
('TB_STK','source'),('TB_IDX','source'),('TB_IDX_DAY','source'),('TB_PRC_DAY','source'),
('TB_FRED_BOND_DAY','source'),('TB_BOND','source'),('TB_BOND_TRD_DAY','source'),
('TB_DERIV','source'),('TB_DERIV_DAY','source'),('TB_MKT_CAL','source'),
('TB_KRX_DATA_ROW','source'),('TB_KRX_CLCT_JOB','source'),('TB_KRX_CLCT_JOB_ITEM','source'),
('TB_KRX_BF_JOB','source'),('TB_KRX_BF_DAY','source'),
('TB_FRED_SERIES','source'),('TB_FRED_OBS','source'),('TB_FRED_OBS_HIST','source'),
('TB_FRED_CLCT_JOB','source'),('TB_FRED_CLCT_JOB_ITEM','source');

-- Source-only V2 investor-flow imports remain intact and become shared inputs.
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['TB_INV_FLOW_DAY','TB_INV_FLOW_FILE','TB_INV_FLOW_RAW_ROW'] LOOP
  IF to_regclass(format('v2.%I',t)) IS NOT NULL THEN
   EXECUTE format('ALTER TABLE v2.%I SET SCHEMA source',t);
   INSERT INTO migration.table_routes VALUES(t,'source');
  END IF;
 END LOOP;
END $$;

-- V1 is the user-approved portfolio authority; all superseded V2 tables remain
-- available in archive_v2, and their values are never erased by this migration.
DO $$ DECLARE r record; BEGIN
 FOR r IN SELECT * FROM migration.table_routes LOOP
  IF to_regclass(format('v1.%I',r.table_name)) IS NOT NULL THEN
   IF to_regclass(format('%I.%I',r.target_schema,r.table_name)) IS NOT NULL THEN
    RAISE EXCEPTION 'Route collision: %',r.table_name;
   END IF;
   EXECUTE format('ALTER TABLE v1.%I SET SCHEMA %I',r.table_name,r.target_schema);
  END IF;
  IF to_regclass(format('v2.%I',r.table_name)) IS NOT NULL THEN
   EXECUTE format('ALTER TABLE v2.%I SET SCHEMA archive_v2',r.table_name);
  END IF;
 END LOOP;
END $$;

-- Prefer existing canonical observations; supplement absent business keys.
-- Preserve all differing observations in the restored archive for review.
DO $$ DECLARE t text; cols text; select_cols text; BEGIN
 PERFORM set_config('work_mem','256MB',true);
 FOREACH t IN ARRAY ARRAY['TB_IDX_DAY','TB_FRED_BOND_DAY','TB_KRX_DATA_ROW'] LOOP
  SELECT string_agg(format('%I',a.attname),',' ORDER BY a.attnum),
         string_agg(format('b.%I',a.attname),',' ORDER BY a.attnum)
    INTO cols,select_cols FROM pg_attribute a
   WHERE a.attrelid=format('source.%I',t)::regclass AND a.attnum>0 AND NOT a.attisdropped
     AND a.attname<>'ID';
  IF t='TB_KRX_DATA_ROW' THEN
   EXECUTE format('INSERT INTO source.%I (%s) SELECT %s FROM archive_v2.%I b WHERE NOT EXISTS (SELECT 1 FROM source.%I a WHERE a."DATA_CD"=b."DATA_CD" AND a."BASE_DT"=b."BASE_DT" AND a."ROW_KEY"=b."ROW_KEY") ON CONFLICT DO NOTHING',t,cols,select_cols,t,t);
  ELSE
   EXECUTE format('INSERT INTO source.%I (%s) SELECT %s FROM archive_v2.%I b WHERE true ON CONFLICT DO NOTHING',t,cols,select_cols,t);
  END IF;
 END LOOP;
END $$;

-- Price IDs refer to stock business keys, never to the old V2 surrogate ID.
DO $$ DECLARE cols text; select_cols text; BEGIN
 SELECT string_agg(format('%I',a.attname),',' ORDER BY a.attnum),
        string_agg(CASE WHEN a.attname='STK_ID' THEN 'm.new_id' ELSE format('b.%I',a.attname) END,',' ORDER BY a.attnum)
 INTO cols,select_cols FROM pg_attribute a WHERE a.attrelid='source."TB_PRC_DAY"'::regclass AND a.attnum>0 AND NOT a.attisdropped;
 EXECUTE format('INSERT INTO source."TB_PRC_DAY" (%s) SELECT %s FROM archive_v2."TB_PRC_DAY" b LEFT JOIN migration.stock_id_map m ON m.old_id=b."STK_ID" ON CONFLICT DO NOTHING',cols,select_cols);
END $$;

-- Remove model FKs to archived inputs, map IDs, then validate against shared tables.
DO $$ DECLARE r record; col text; def text; target text; BEGIN
 FOR r IN SELECT f.*,tr.target_schema FROM migration.foreign_keys f JOIN migration.table_routes tr ON tr.table_name=f.referenced_table
          WHERE to_regclass(format('v2.%I',f.table_name)) IS NOT NULL LOOP
  EXECUTE format('ALTER TABLE v2.%I DROP CONSTRAINT %I',r.table_name,r.conname);
 END LOOP;
 FOR r IN SELECT DISTINCT f.table_name,a.attname AS column_name FROM migration.foreign_keys f
          JOIN pg_attribute a ON a.attrelid=to_regclass(format('v2.%I',f.table_name)) AND a.attnum=ANY(f.conkey)
          WHERE f.referenced_table='TB_STK' AND a.attname='STK_ID' LOOP
  EXECUTE format('UPDATE v2.%I x SET %I=-m.new_id FROM migration.stock_id_map m WHERE x.%I=m.old_id AND m.old_id<>m.new_id',r.table_name,r.column_name,r.column_name);
  EXECUTE format('UPDATE v2.%I SET %I=-%I WHERE %I<0',r.table_name,r.column_name,r.column_name,r.column_name);
 END LOOP;
 FOR r IN SELECT f.*,tr.target_schema FROM migration.foreign_keys f JOIN migration.table_routes tr ON tr.table_name=f.referenced_table
          WHERE to_regclass(format('v2.%I',f.table_name)) IS NOT NULL LOOP
  def:=regexp_replace(r.definition,'REFERENCES [^\(]+\(',format('REFERENCES %I.%I(',r.target_schema,r.referenced_table));
  EXECUTE format('ALTER TABLE v2.%I ADD CONSTRAINT %I %s',r.table_name,r.conname,def);
 END LOOP;
END $$;

-- Shared collector tables and exchange history must not be shadowed by local copies.
DO $$ DECLARE r record; e text; BEGIN
 FOR r IN SELECT relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
          WHERE n.nspname='source' AND c.relkind='r' AND (relname LIKE 'TB_KOFIA_%' OR relname='TB_EXCH_DAY') LOOP
  FOREACH e IN ARRAY ARRAY['v1','v2'] LOOP
   IF to_regclass(format('%I.%I',e,r.relname)) IS NOT NULL THEN
    EXECUTE format('ALTER TABLE %I.%I SET SCHEMA %I',e,r.relname,'archive_'||e);
   END IF;
  END LOOP;
 END LOOP;
END $$;

-- Existing model SQL retains TB_HOLD/TB_REG_BUY names, but these become views
-- over one physical portfolio and separate per-model calculation fields.
CREATE FUNCTION migration.split_model_view(env text,t text,keys text[],model_columns text[]) RETURNS void LANGUAGE plpgsql AS $$
DECLARE projection text; key_list text; model_list text; select_models text; key_match text;
        all_cols text; values_core text; values_model text; update_core text; update_model text;
        fn text; seq text; id_col text; defaults text; old_schema text;
BEGIN
 old_schema:=CASE WHEN env='v1' THEN 'portfolio' ELSE 'archive_v2' END;
 SELECT string_agg(format('%I',c),',') INTO key_list FROM unnest(keys) c;
 SELECT string_agg(format('%I',c),',') INTO model_list FROM unnest(model_columns) c;
 SELECT string_agg(format('m.%I',c),',') INTO select_models FROM unnest(model_columns) c;
 EXECUTE format('CREATE TABLE %I.%I AS SELECT %s,%s FROM %I.%I',env,t||'_MODEL',key_list,model_list,old_schema,t);
 EXECUTE format('ALTER TABLE %I.%I ADD PRIMARY KEY (%s)',env,t||'_MODEL',key_list);
 SELECT string_agg(format('p.%I=m.%I',c,c),' AND ') INTO key_match FROM unnest(keys) c;
 SELECT string_agg(CASE WHEN a.attname=ANY(model_columns) THEN format('m.%I',a.attname) ELSE format('p.%I',a.attname) END,',' ORDER BY a.attnum),
        string_agg(format('%I',a.attname),',' ORDER BY a.attnum),
        string_agg(format('NEW.%I',a.attname),',' ORDER BY a.attnum),
        string_agg(format('%I=NEW.%I',a.attname,a.attname),',' ORDER BY a.attnum) FILTER(WHERE a.attname<>ALL(keys) AND a.attname<>ALL(model_columns))
 INTO projection,all_cols,values_core,update_core
 FROM pg_attribute a WHERE a.attrelid=format('portfolio.%I',t)::regclass AND a.attnum>0 AND NOT a.attisdropped;
 EXECUTE format('CREATE VIEW %I.%I AS SELECT %s FROM portfolio.%I p LEFT JOIN %I.%I m ON %s',env,t,projection,t,env,t||'_MODEL',key_match);
 SELECT string_agg(format('NEW.%I',c),',') INTO values_model FROM unnest(keys||model_columns) c;
 SELECT string_agg(format('%I=EXCLUDED.%I',c,c),',') INTO update_model FROM unnest(model_columns) c;
 SELECT string_agg(format('p.%I=OLD.%I',c,c),' AND ') INTO key_match FROM unnest(keys) c;
 defaults:='';
 IF t='TB_HOLD' THEN
  defaults:=format('IF NEW."HOLD_ID" IS NULL THEN NEW."HOLD_ID":=nextval(%L); END IF;',pg_get_serial_sequence('portfolio."TB_HOLD"','HOLD_ID'));
 END IF;
 FOR id_col,seq IN SELECT a.attname,pg_get_expr(d.adbin,d.adrelid) FROM pg_attribute a
   JOIN pg_attrdef d ON d.adrelid=a.attrelid AND d.adnum=a.attnum
   WHERE a.attrelid=format('portfolio.%I',t)::regclass LOOP
   defaults:=defaults||format('IF NEW.%I IS NULL THEN NEW.%I:=%s; END IF;',id_col,id_col,seq);
 END LOOP;
 fn:=format($body$
 CREATE FUNCTION %I.%I() RETURNS trigger LANGUAGE plpgsql AS $trigger$
 BEGIN
  IF TG_OP='DELETE' THEN DELETE FROM portfolio.%I p WHERE %s; RETURN OLD; END IF;
  IF TG_OP='INSERT' THEN %s INSERT INTO portfolio.%I (%s) VALUES (%s);
  ELSE UPDATE portfolio.%I p SET %s WHERE %s; END IF;
  INSERT INTO %I.%I (%s,%s) VALUES (%s) ON CONFLICT (%s) DO UPDATE SET %s;
  RETURN NEW;
 END $trigger$;
 $body$,env,t||'_write',t,key_match,defaults,t,all_cols,values_core,t,update_core,key_match,env,t||'_MODEL',key_list,model_list,values_model,key_list,update_model);
 EXECUTE fn;
 EXECUTE format('CREATE TRIGGER model_write INSTEAD OF INSERT OR UPDATE OR DELETE ON %I.%I FOR EACH ROW EXECUTE FUNCTION %I.%I()',env,t,env,t||'_write');
END $$;
SELECT migration.split_model_view(e,'TB_HOLD',ARRAY['HOLD_ID'],ARRAY['TGT_WGT','WGT_DIFF_RT','WGT_STS','RISK_GRADE']) FROM unnest(ARRAY['v1','v2']) e;
SELECT migration.split_model_view(e,'TB_REG_BUY',ARRAY['ACCT_TP','STK_CD'],ARRAY['MKT_MULT','STK_ADJ_MULT','WGT_ADJ_MULT','RISK_ADJ_MULT','FINAL_MULT','BASE_BUY_AMT','RCMD_BUY_AMT','SAVED_AMT','ACT_SIG','LAST_CALC_DT','RULE_VER_NO']) FROM unnest(ARRAY['v1','v2']) e;
COMMIT;
