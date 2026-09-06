DELETE FROM "TB_KOFIA_DATA_ROW" target
 WHERE target."DATASET_CD" IN (
           'FINAL_QUOTED_YIELD',
           'CMA_DAILY_STATUS',
           'PRIVATE_EQUITY_COMPANY_SCALE'
       )
   AND NOT EXISTS (
       SELECT 1
         FROM "TB_KOFIA_DATA_ROW" calendar
        WHERE calendar."DATASET_CD" = 'KOSPI_MARKET'
          AND calendar."BASE_DT" = target."BASE_DT"
   );
