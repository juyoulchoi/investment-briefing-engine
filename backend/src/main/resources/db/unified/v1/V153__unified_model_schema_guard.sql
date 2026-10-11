DO $$ BEGIN
 IF current_schema()<>'v1' OR to_regclass('portfolio."TB_ACCT"') IS NULL
     OR (SELECT relkind FROM pg_class WHERE oid='v1."TB_HOLD"'::regclass)<>'v' THEN
  RAISE EXCEPTION 'Unified V1 bootstrap required';
 END IF;
END $$;
