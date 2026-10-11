\set ON_ERROR_STOP on
BEGIN;
DO $$ DECLARE r record; definition text; BEGIN
 IF current_database() NOT LIKE 'investment_unified_%' THEN RAISE EXCEPTION 'Candidate database required'; END IF;
 FOR r IN SELECT tr.table_name,a.attname,format_type(a.atttypid,a.atttypmod) AS type,
                 pg_get_expr(d.adbin,d.adrelid) AS default_expression
          FROM migration.table_routes tr
          JOIN pg_attribute a ON a.attrelid=to_regclass(format('archive_v2.%I',tr.table_name))
             AND a.attnum>0 AND NOT a.attisdropped
          LEFT JOIN pg_attrdef d ON d.adrelid=a.attrelid AND d.adnum=a.attnum
          WHERE tr.target_schema='source' AND NOT EXISTS (
            SELECT 1 FROM pg_attribute x WHERE x.attrelid=to_regclass(format('source.%I',tr.table_name))
              AND x.attname=a.attname AND x.attnum>0 AND NOT x.attisdropped)
          ORDER BY tr.table_name,a.attnum LOOP
  definition:=format('ALTER TABLE source.%I ADD COLUMN %I %s',r.table_name,r.attname,r.type);
  IF r.default_expression IS NOT NULL AND r.default_expression NOT LIKE '%nextval%'
       AND r.default_expression NOT LIKE '%archive_%' THEN
   definition:=definition||' DEFAULT '||r.default_expression;
  END IF;
  EXECUTE definition;
  RAISE NOTICE 'Added V2 source metadata %.%',r.table_name,r.attname;
 END LOOP;
END $$;
COMMIT;
