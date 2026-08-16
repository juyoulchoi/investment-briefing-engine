CREATE TEMPORARY TABLE "TMP_OVERSEAS_STOCK_NAME" (
    "STK_CD" VARCHAR(30) PRIMARY KEY,
    "STK_NM" VARCHAR(150) NOT NULL
);

INSERT INTO "TMP_OVERSEAS_STOCK_NAME" ("STK_CD", "STK_NM")
VALUES
    ('BOTZ', 'BOTZ'),
    ('GEV', 'GE 버노바'),
    ('HYDR', 'HYDR'),
    ('QQQ', 'QQQ'),
    ('SCHD', 'SCHD'),
    ('SMH', 'SMH'),
    ('SPY', 'SPY'),
    ('VIG', 'VIG'),
    ('XLF', 'XLF'),
    ('XLI', 'XLI'),
    ('XLV', 'XLV'),
    ('MSFT', '마이크로소프트'),
    ('BAC', '뱅크오브아메리카'),
    ('BRK.B', '버크셔 해서웨이 B'),
    ('VRT', '버티브 홀딩스'),
    ('AVGO', '브로드컴'),
    ('VST', '비스트라 에너지'),
    ('V', '비자'),
    ('SPCX', '스페이스X'),
    ('ANET', '아리스타 네트웍스'),
    ('AMZN', '아마존닷컴'),
    ('IONQ', '아이온큐'),
    ('GOOGL', '알파벳A'),
    ('ABBV', '애브비'),
    ('AAPL', '애플'),
    ('NVDA', '엔비디아'),
    ('WMT', '월마트'),
    ('INTC', '인텔'),
    ('LLY', '일라이 릴리'),
    ('JPM', '제이피모간체이스'),
    ('JNJ', '존슨앤드존슨'),
    ('CAT', '캐터필러'),
    ('COST', '코스트코 홀세일'),
    ('CEG', '콘스텔레이션 에너지'),
    ('PLTR', '팔란티어'),
    ('PLUG', '플러그파워');

DO $$
DECLARE
    V_TARGET_COUNT INTEGER;
    V_STOCK_COUNT INTEGER;
    V_REG_BUY_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_TARGET_COUNT FROM "TMP_OVERSEAS_STOCK_NAME";

    SELECT COUNT(*) INTO V_STOCK_COUNT
      FROM "TMP_OVERSEAS_STOCK_NAME" t
      JOIN "TB_STK" s
        ON s."MKT_CD" = 'US'
       AND s."STK_CD" = t."STK_CD";

    SELECT COUNT(*) INTO V_REG_BUY_COUNT
      FROM "TMP_OVERSEAS_STOCK_NAME" t
      JOIN "TB_REG_BUY" r
        ON r."ACCT_TP" = 'OVERSEAS'
       AND r."STK_CD" = t."STK_CD"
       AND r."DEL_YN" = 'N';

    IF V_TARGET_COUNT <> 36
       OR V_STOCK_COUNT <> V_TARGET_COUNT
       OR V_REG_BUY_COUNT <> V_TARGET_COUNT THEN
        RAISE EXCEPTION
            'Overseas stock-name target mismatch: targets %, stocks %, regular buys %',
            V_TARGET_COUNT, V_STOCK_COUNT, V_REG_BUY_COUNT;
    END IF;
END $$;

UPDATE "TB_STK" s
   SET "STK_NM" = t."STK_NM",
       "UPD_DTTM" = CURRENT_TIMESTAMP,
       "UPD_USR_ID" = 'SYSTEM'
  FROM "TMP_OVERSEAS_STOCK_NAME" t
 WHERE s."MKT_CD" = 'US'
   AND s."STK_CD" = t."STK_CD";

UPDATE "TB_REG_BUY" r
   SET "STK_NM" = t."STK_NM",
       "MOD_DT" = CURRENT_TIMESTAMP,
       "UPD_DTTM" = CURRENT_TIMESTAMP,
       "UPD_USR_ID" = 'SYSTEM'
  FROM "TMP_OVERSEAS_STOCK_NAME" t
 WHERE r."ACCT_TP" = 'OVERSEAS'
   AND r."STK_CD" = t."STK_CD"
   AND r."DEL_YN" = 'N';

DO $$
DECLARE
    V_STOCK_MISMATCH_COUNT INTEGER;
    V_REG_BUY_MISMATCH_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_STOCK_MISMATCH_COUNT
      FROM "TMP_OVERSEAS_STOCK_NAME" t
      JOIN "TB_STK" s
        ON s."MKT_CD" = 'US'
       AND s."STK_CD" = t."STK_CD"
     WHERE s."STK_NM" <> t."STK_NM";

    SELECT COUNT(*) INTO V_REG_BUY_MISMATCH_COUNT
      FROM "TMP_OVERSEAS_STOCK_NAME" t
      JOIN "TB_REG_BUY" r
        ON r."ACCT_TP" = 'OVERSEAS'
       AND r."STK_CD" = t."STK_CD"
       AND r."DEL_YN" = 'N'
     WHERE r."STK_NM" <> t."STK_NM";

    IF V_STOCK_MISMATCH_COUNT <> 0 OR V_REG_BUY_MISMATCH_COUNT <> 0 THEN
        RAISE EXCEPTION
            'Overseas stock-name verification failed: stock mismatches %, regular-buy mismatches %',
            V_STOCK_MISMATCH_COUNT, V_REG_BUY_MISMATCH_COUNT;
    END IF;
END $$;
