UPDATE "TB_REG_BUY" r
SET "ACCT_ID" = domestic."ACCT_ID",
    "UPD_DTTM" = CURRENT_TIMESTAMP,
    "UPD_USR_ID" = 'SYSTEM'
FROM "TB_STK" s,
     LATERAL (
         SELECT a."ACCT_ID"
         FROM "TB_ACCT" a
         WHERE a."ACCT_TP" = 'DOMESTIC'
           AND a."USE_YN" = 'Y'
           AND a."DEL_YN" = 'N'
         ORDER BY a."DISP_SEQ", a."ACCT_ID"
         LIMIT 1
     ) domestic
WHERE r."STK_ID" = s."STK_ID"
  AND s."STK_CD" = '000660'
  AND r."DEL_YN" = 'N'
  AND EXISTS (
      SELECT 1
      FROM "TB_ACCT" current_account
      WHERE current_account."ACCT_ID" = r."ACCT_ID"
        AND current_account."ACCT_TP" = 'OVERSEAS'
  );

