CREATE TEMP TABLE TMP_OVERSEAS_STOCK_NAME (
    STK_CD VARCHAR(50) PRIMARY KEY,
    STK_NM VARCHAR(200) NOT NULL
) ON COMMIT DROP;

INSERT INTO TMP_OVERSEAS_STOCK_NAME (STK_CD, STK_NM) VALUES
    ('BOTZ', 'BOTZ'),
    ('HYDR', 'HYDR'),
    ('QQQ', 'QQQ'),
    ('SCHD', 'SCHD'),
    ('SMH', 'SMH'),
    ('SPY', 'SPY'),
    ('VIG', 'VIG'),
    ('XLF', 'XLF'),
    ('XLI', 'XLI'),
    ('XLV', 'XLV'),
    ('VRT', '버티브 홀딩스'),
    ('ANET', '아리스타 네트웍스'),
    ('IONQ', '아이온큐'),
    ('ABBV', '애브비'),
    ('NVDA', '엔비디아'),
    ('WMT', '월마트'),
    ('INTC', '인텔'),
    ('JPM', '제이피모간체이스'),
    ('JNJ', '존슨앤드존슨'),
    ('CAT', '캐터필러'),
    ('COST', '코스트코 홀세일'),
    ('CEG', '콘스텔레이션 에너지'),
    ('PLTR', '팔란티어'),
    ('PLUG', '플러그파워');

DO $$
DECLARE
    TARGET_COUNT INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO TARGET_COUNT
      FROM "TB_STK" S
      JOIN TMP_OVERSEAS_STOCK_NAME T ON T.STK_CD = S."STK_CD"
     WHERE S."LIST_SCOPE" = 'OVERSEAS';

    IF TARGET_COUNT <> 24 THEN
        RAISE EXCEPTION 'Expected 24 overseas stocks for name update, found %', TARGET_COUNT;
    END IF;
END $$;

UPDATE "TB_STK" S
   SET "STK_NM" = T.STK_NM,
       "UPD_DTTM" = CURRENT_TIMESTAMP
  FROM TMP_OVERSEAS_STOCK_NAME T
 WHERE S."STK_CD" = T.STK_CD
   AND S."LIST_SCOPE" = 'OVERSEAS'
   AND S."STK_NM" IS DISTINCT FROM T.STK_NM;

DO $$
DECLARE
    MISMATCH_COUNT INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO MISMATCH_COUNT
      FROM TMP_OVERSEAS_STOCK_NAME T
      LEFT JOIN "TB_STK" S
        ON S."STK_CD" = T.STK_CD
       AND S."LIST_SCOPE" = 'OVERSEAS'
     WHERE S."STK_ID" IS NULL
        OR S."STK_NM" IS DISTINCT FROM T.STK_NM;

    IF MISMATCH_COUNT <> 0 THEN
        RAISE EXCEPTION 'Overseas stock name update left % mismatches', MISMATCH_COUNT;
    END IF;
END $$;
