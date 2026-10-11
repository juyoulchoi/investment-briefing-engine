\set ON_ERROR_STOP on
BEGIN;
DO $$ BEGIN
 IF current_database() <> 'investment_unified_live' THEN RAISE EXCEPTION 'Live unified database required'; END IF;
 IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='unified_backend') THEN
  CREATE ROLE unified_backend NOLOGIN;
 END IF;
END $$;
GRANT unified_v1,unified_collector TO unified_backend;
COMMIT;
