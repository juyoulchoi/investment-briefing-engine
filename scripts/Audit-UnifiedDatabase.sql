\set ON_ERROR_STOP on
SELECT n.nspname AS schema,count(*) AS tables
FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE c.relkind='r' AND n.nspname IN ('source','portfolio','v1','v2') GROUP BY n.nspname ORDER BY 1;

-- Surrogate IDs must be mapped by business keys before sharing references.
SELECT 'ACCOUNT_ID' AS issue,a."ACCT_TP" AS business_key,a."ACCT_ID" AS v1_id,b."ACCT_ID" AS v2_id
FROM v1."TB_ACCT" a JOIN v2."TB_ACCT" b USING ("ACCT_TP") WHERE a."ACCT_ID"<>b."ACCT_ID";
SELECT 'STOCK_ID' AS issue,a."MKT_CD",a."STK_CD",a."STK_ID" AS v1_id,b."STK_ID" AS v2_id
FROM v1."TB_STK" a JOIN v2."TB_STK" b USING ("MKT_CD","STK_CD") WHERE a."STK_ID"<>b."STK_ID";
SELECT 'HOLDING_ID' AS issue,a."ACCT_TP",a."MKT_CD",a."STK_CD",a."HOLD_ID" AS v1_id,b."HOLD_ID" AS v2_id
FROM v1."TB_HOLD" a JOIN v2."TB_HOLD" b USING ("ACCT_TP","MKT_CD","STK_CD") WHERE a."HOLD_ID"<>b."HOLD_ID";

SELECT coalesce(a."ACCT_TP",b."ACCT_TP") AS account_type,a."CASH_AMT" AS v1_cash,b."CASH_AMT" AS v2_cash,
       a."AVAIL_CASH" AS v1_available,b."AVAIL_CASH" AS v2_available
FROM v1."TB_ACCT" a FULL JOIN v2."TB_ACCT" b USING ("ACCT_TP") ORDER BY 1;
SELECT coalesce(a."ACCT_TP",b."ACCT_TP") AS account_type,coalesce(a."MKT_CD",b."MKT_CD") AS market,
       coalesce(a."STK_CD",b."STK_CD") AS stock,a."HOLD_QTY" AS v1_quantity,b."HOLD_QTY" AS v2_quantity,
       a."AVG_PRC" AS v1_average_price,b."AVG_PRC" AS v2_average_price
FROM v1."TB_HOLD" a FULL JOIN v2."TB_HOLD" b USING ("ACCT_TP","MKT_CD","STK_CD")
WHERE a."HOLD_QTY" IS DISTINCT FROM b."HOLD_QTY" OR a."AVG_PRC" IS DISTINCT FROM b."AVG_PRC" ORDER BY 1,2,3;

-- Capture exact composite foreign keys, avoiding information_schema cross-products.
SELECT n.nspname AS schema,c.relname AS table_name,k.conname,pg_get_constraintdef(k.oid) AS definition,
       rn.nspname AS referenced_schema,rc.relname AS referenced_table
FROM pg_constraint k JOIN pg_class c ON c.oid=k.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace
JOIN pg_class rc ON rc.oid=k.confrelid JOIN pg_namespace rn ON rn.oid=rc.relnamespace
WHERE k.contype='f' AND n.nspname IN ('v1','v2') ORDER BY 1,2,3;

-- Applied migrations differ: retain independent histories for model schemas.
SELECT 'v1' AS environment,version,description FROM v1.flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 5;
SELECT 'v2' AS environment,version,description FROM v2.flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 5;
