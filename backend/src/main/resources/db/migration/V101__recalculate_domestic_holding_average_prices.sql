WITH CALCULATED AS (
    SELECT "HOLD_ID",
           CASE
               WHEN "HOLD_QTY" = 0 THEN 0::NUMERIC
               ELSE ROUND(("WHOLE_BUY_AMT" + "FRAC_BUY_AMT") / "HOLD_QTY", 6)
           END AS "AVG_PRC"
      FROM "TB_HOLD"
     WHERE "ACCT_TP" = 'DOMESTIC'
)
UPDATE "TB_HOLD" H
   SET "AVG_PRC" = C."AVG_PRC",
       "ORG_PL_AMT" = CASE
           WHEN H."CUR_PRC" IS NULL THEN NULL
           ELSE (H."CUR_PRC" - C."AVG_PRC") * H."HOLD_QTY"
       END,
       "PL_AMT" = CASE
           WHEN H."CUR_PRC" IS NULL THEN NULL
           ELSE (H."CUR_PRC" - C."AVG_PRC") * H."HOLD_QTY" * H."EXCH_RT"
       END,
       "PL_RT" = CASE
           WHEN H."CUR_PRC" IS NULL OR C."AVG_PRC" = 0 THEN NULL
           ELSE (H."CUR_PRC" - C."AVG_PRC") * 100 / C."AVG_PRC"
       END,
       "CALC_DTTM" = CURRENT_TIMESTAMP
  FROM CALCULATED C
 WHERE C."HOLD_ID" = H."HOLD_ID";
