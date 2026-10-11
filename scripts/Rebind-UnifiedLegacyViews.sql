\set ON_ERROR_STOP on
BEGIN;
DO $$ DECLARE r record; route record; definition text; e text; target text; BEGIN
 IF current_database() NOT LIKE 'investment_unified_%' THEN RAISE EXCEPTION 'Candidate database required'; END IF;
 -- PostgreSQL keeps view bindings to a table when it moves schemas. Rebind every
 -- legacy lowercase compatibility view, including the original stock/price APIs.
 FOR r IN SELECT n.nspname AS schema,c.relname,pg_get_viewdef(c.oid,true) AS definition
          FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
          WHERE c.relkind='v' AND n.nspname IN ('v1','v2') AND c.relname=lower(c.relname)
          ORDER BY n.nspname,c.relname LOOP
  definition:=r.definition;
  FOR route IN SELECT * FROM migration.table_routes LOOP
   target:=CASE WHEN route.table_name IN ('TB_HOLD','TB_REG_BUY') THEN r.schema ELSE route.target_schema END;
   FOREACH e IN ARRAY ARRAY['archive_v1','archive_v2','portfolio','v1','v2'] LOOP
    definition:=replace(definition,format('%I.%I',e,route.table_name),format('%I.%I',target,route.table_name));
   END LOOP;
  END LOOP;
  FOR route IN SELECT relname AS table_name FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
               WHERE n.nspname='source' AND c.relkind='r' AND (relname LIKE 'TB_KOFIA_%' OR relname='TB_EXCH_DAY') LOOP
   FOREACH e IN ARRAY ARRAY['archive_v1','archive_v2','v1','v2'] LOOP
    definition:=replace(definition,format('%I.%I',e,route.table_name),format('source.%I',route.table_name));
   END LOOP;
  END LOOP;
  EXECUTE format('CREATE OR REPLACE VIEW %I.%I AS %s',r.schema,r.relname,definition);
 END LOOP;
END $$;
COMMIT;
