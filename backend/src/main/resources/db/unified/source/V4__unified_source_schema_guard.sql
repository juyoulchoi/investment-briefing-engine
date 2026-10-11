DO $$ BEGIN
 IF to_regclass('source."TB_PRC_DAY"') IS NULL OR to_regclass('portfolio."TB_HOLD"') IS NULL THEN
  RAISE EXCEPTION 'Unified source and portfolio bootstrap required before collector migration';
 END IF;
END $$;
