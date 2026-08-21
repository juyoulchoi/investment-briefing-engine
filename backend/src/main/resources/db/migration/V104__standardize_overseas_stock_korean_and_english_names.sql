CREATE TEMPORARY TABLE "TMP_OVERSEAS_STOCK_NAMES" (
    "STK_CD" VARCHAR(30) PRIMARY KEY,
    "STK_NM" VARCHAR(150) NOT NULL,
    "STK_NM_EN" VARCHAR(150) NOT NULL
);

INSERT INTO "TMP_OVERSEAS_STOCK_NAMES" ("STK_CD", "STK_NM", "STK_NM_EN")
VALUES
    ('BOTZ', 'BOTZ', 'Global X Robotics & Artificial Intelligence ETF'),
    ('HYDR', 'HYDR', 'Global X Hydrogen ETF'),
    ('QQQ', 'QQQ', 'Invesco QQQ Trust'),
    ('SCHD', 'SCHD', 'Schwab U.S. Dividend Equity ETF'),
    ('SMH', 'SMH', 'VanEck Semiconductor ETF'),
    ('SPY', 'SPY', 'State Street SPDR S&P 500 ETF Trust'),
    ('VIG', 'VIG', 'Vanguard Dividend Appreciation Index Fund ETF Shares'),
    ('XLF', 'XLF', 'State Street Financial Select Sector SPDR ETF'),
    ('XLI', 'XLI', 'State Street Industrial Select Sector SPDR ETF'),
    ('XLV', 'XLV', 'State Street Health Care Select Sector SPDR ETF'),
    ('GEV', 'GE 버노바', 'GE Vernova Inc.'),
    ('MSFT', '마이크로소프트', 'Microsoft Corporation'),
    ('BAC', '뱅크오브아메리카', 'Bank of America Corporation'),
    ('BRK.B', '버크셔 해서웨이 B', 'Berkshire Hathaway Inc.'),
    ('VRT', '버티브 홀딩스', 'Vertiv Holdings Co'),
    ('AVGO', '브로드컴', 'Broadcom Inc.'),
    ('VST', '비스트라 에너지', 'Vistra Corp.'),
    ('V', '비자', 'Visa Inc.'),
    ('SPCX', '스페이스X', 'Space Exploration Technologies Corp.'),
    ('ANET', '아리스타 네트웍스', 'Arista Networks, Inc.'),
    ('AMZN', '아마존닷컴', 'Amazon.com, Inc.'),
    ('IONQ', '아이온큐', 'IonQ, Inc.'),
    ('GOOGL', '알파벳A', 'Alphabet Inc.'),
    ('ABBV', '애브비', 'AbbVie Inc.'),
    ('AAPL', '애플', 'Apple Inc.'),
    ('NVDA', '엔비디아', 'NVIDIA Corporation'),
    ('WMT', '월마트', 'Walmart Inc.'),
    ('INTC', '인텔', 'Intel Corporation'),
    ('LLY', '일라이 릴리', 'Eli Lilly and Company'),
    ('JPM', '제이피모간체이스', 'JPMorgan Chase & Co.'),
    ('JNJ', '존슨앤드존슨', 'Johnson & Johnson'),
    ('CAT', '캐터필러', 'Caterpillar Inc.'),
    ('COST', '코스트코 홀세일', 'Costco Wholesale Corporation'),
    ('CEG', '콘스텔레이션 에너지', 'Constellation Energy Corporation'),
    ('PLTR', '팔란티어', 'Palantir Technologies Inc.'),
    ('PLUG', '플러그파워', 'Plug Power Inc.');

DO $$
DECLARE
    V_TARGET_COUNT INTEGER;
    V_OVERSEAS_COUNT INTEGER;
    V_MATCHED_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_TARGET_COUNT FROM "TMP_OVERSEAS_STOCK_NAMES";
    SELECT COUNT(*) INTO V_OVERSEAS_COUNT
      FROM "TB_STK"
     WHERE "LIST_SCOPE" = 'OVERSEAS';
    SELECT COUNT(*) INTO V_MATCHED_COUNT
      FROM "TMP_OVERSEAS_STOCK_NAMES" t
      JOIN "TB_STK" s
        ON s."LIST_SCOPE" = 'OVERSEAS'
       AND s."STK_CD" = t."STK_CD";

    IF V_TARGET_COUNT <> 36
       OR V_OVERSEAS_COUNT <> V_TARGET_COUNT
       OR V_MATCHED_COUNT <> V_TARGET_COUNT THEN
        RAISE EXCEPTION
            'Overseas stock-name target mismatch: targets %, overseas stocks %, matched %',
            V_TARGET_COUNT, V_OVERSEAS_COUNT, V_MATCHED_COUNT;
    END IF;
END $$;

UPDATE "TB_STK" s
   SET "STK_NM" = t."STK_NM",
       "STK_NM_EN" = t."STK_NM_EN",
       "UPD_DTTM" = CURRENT_TIMESTAMP,
       "UPD_USR_ID" = 'SYSTEM'
  FROM "TMP_OVERSEAS_STOCK_NAMES" t
 WHERE s."LIST_SCOPE" = 'OVERSEAS'
   AND s."STK_CD" = t."STK_CD";

DO $$
DECLARE
    V_MISMATCH_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_MISMATCH_COUNT
      FROM "TMP_OVERSEAS_STOCK_NAMES" t
      JOIN "TB_STK" s
        ON s."LIST_SCOPE" = 'OVERSEAS'
       AND s."STK_CD" = t."STK_CD"
     WHERE s."STK_NM" <> t."STK_NM"
        OR s."STK_NM_EN" <> t."STK_NM_EN";

    IF V_MISMATCH_COUNT <> 0 THEN
        RAISE EXCEPTION
            'Overseas stock-name verification failed: mismatches %',
            V_MISMATCH_COUNT;
    END IF;
END $$;
