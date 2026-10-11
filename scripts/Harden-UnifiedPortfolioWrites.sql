\set ON_ERROR_STOP on
BEGIN;
DO $$ DECLARE e text; t text; definition text; sync text; BEGIN
 IF current_database() NOT LIKE 'investment_unified_%' THEN RAISE EXCEPTION 'Unified database required'; END IF;
 FOREACH e IN ARRAY ARRAY['v1','v2'] LOOP
  FOREACH t IN ARRAY ARRAY['TB_HOLD','TB_REG_BUY'] LOOP
   definition:=pg_get_functiondef(format('%I.%I()',e,t||'_write')::regprocedure);
   IF definition LIKE '%unified_business_key_sync%' THEN CONTINUE; END IF;
   sync:=$body$BEGIN
    -- unified_business_key_sync: ORM uses IDs while legacy queries use business keys.
    IF TG_OP <> 'DELETE' THEN
     NEW."ACCT_TP":=(SELECT "ACCT_TP" FROM portfolio."TB_ACCT" WHERE "ACCT_ID"=NEW."ACCT_ID");
     NEW."MKT_CD":=(SELECT "MKT_CD" FROM source."TB_STK" WHERE "STK_ID"=NEW."STK_ID");
     NEW."STK_CD":=(SELECT "STK_CD" FROM source."TB_STK" WHERE "STK_ID"=NEW."STK_ID");
    END IF;
   $body$;
   EXECUTE regexp_replace(definition,'BEGIN',sync);
  END LOOP;
 END LOOP;
END $$;
COMMIT;
