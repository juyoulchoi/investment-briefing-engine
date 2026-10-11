\set ON_ERROR_STOP on
BEGIN;
DO $$ DECLARE r text; BEGIN
 IF current_database() NOT LIKE 'investment_unified_%' THEN RAISE EXCEPTION 'Unified database required'; END IF;
 FOREACH r IN ARRAY ARRAY['unified_v1','unified_v2','unified_collector'] LOOP
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname=r) THEN EXECUTE format('CREATE ROLE %I NOLOGIN',r); END IF;
 END LOOP;
END $$;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA v1,portfolio,source TO unified_v1;
GRANT USAGE ON SCHEMA v2,portfolio,source TO unified_v2;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA v1 TO unified_v1;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA v2 TO unified_v2;
GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA v1 TO unified_v1;
GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA v2 TO unified_v2;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA portfolio TO unified_v1,unified_v2;
GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA portfolio TO unified_v1,unified_v2;
GRANT SELECT ON ALL TABLES IN SCHEMA source TO unified_v1,unified_v2;
-- Reference master administration and the V2 Excel importer are explicit input writers.
GRANT INSERT,UPDATE,DELETE ON source."TB_STK",source."TB_IDX" TO unified_v1,unified_v2;
GRANT INSERT,UPDATE,DELETE ON source."TB_INV_FLOW_DAY",source."TB_INV_FLOW_FILE",source."TB_INV_FLOW_RAW_ROW" TO unified_v2;
DO $$ DECLARE r record; BEGIN
 FOR r IN SELECT pg_get_serial_sequence('source."TB_STK"','STK_ID') AS seq LOOP
  IF r.seq IS NOT NULL THEN EXECUTE format('GRANT USAGE,SELECT ON SEQUENCE %s TO unified_v1,unified_v2',r.seq); END IF;
 END LOOP;
END $$;
GRANT USAGE ON SCHEMA source,portfolio TO unified_collector;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA source TO unified_collector;
GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA source TO unified_collector;
GRANT SELECT ON ALL TABLES IN SCHEMA portfolio TO unified_collector;
GRANT UPDATE("CUR_PRC","PRC_BASE_DT","ORG_EVL_AMT","EVL_AMT","ORG_PL_AMT","PL_AMT","PL_RT","CALC_DTTM") ON portfolio."TB_HOLD" TO unified_collector;
GRANT USAGE ON SCHEMA source TO exchange_reader;
GRANT SELECT ON source."TB_EXCH_DAY" TO exchange_reader;
-- New objects created by bootstrap/migrations keep the same role boundary.
ALTER DEFAULT PRIVILEGES FOR ROLE source_owner IN SCHEMA source GRANT SELECT ON TABLES TO unified_v1,unified_v2;
ALTER DEFAULT PRIVILEGES FOR ROLE source_owner IN SCHEMA source GRANT ALL ON TABLES TO unified_collector;
ALTER DEFAULT PRIVILEGES FOR ROLE source_owner IN SCHEMA source GRANT USAGE,SELECT ON SEQUENCES TO unified_collector;
ALTER DEFAULT PRIVILEGES FOR ROLE source_owner IN SCHEMA v1 GRANT ALL ON TABLES TO unified_v1;
ALTER DEFAULT PRIVILEGES FOR ROLE source_owner IN SCHEMA v2 GRANT ALL ON TABLES TO unified_v2;
ALTER DEFAULT PRIVILEGES FOR ROLE source_owner IN SCHEMA v1 GRANT USAGE,SELECT ON SEQUENCES TO unified_v1;
ALTER DEFAULT PRIVILEGES FOR ROLE source_owner IN SCHEMA v2 GRANT USAGE,SELECT ON SEQUENCES TO unified_v2;
COMMIT;
