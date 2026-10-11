DO $$ BEGIN
 IF current_schema()<>'v2' OR to_regclass('portfolio."TB_ACCT"') IS NULL
     OR (SELECT relkind FROM pg_class WHERE oid='v2."TB_HOLD"'::regclass)<>'v' THEN
  RAISE EXCEPTION 'Unified V2 bootstrap required';
 END IF;
END $$;
