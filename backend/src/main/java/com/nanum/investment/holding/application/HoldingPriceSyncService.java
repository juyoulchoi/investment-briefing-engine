package com.nanum.investment.holding.application;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class HoldingPriceSyncService {
  private final JdbcClient jdbc;

  public HoldingPriceSyncService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public int refreshMarket(String marketCode) {
    return refresh(marketCode, null);
  }

  public int refreshStock(String marketCode, String stockCode) {
    return refresh(marketCode, stockCode);
  }

  private int refresh(String marketCode, String stockCode) {
    return jdbc.sql(
            """
                WITH latest_price AS (
                    SELECT DISTINCT ON ("STK_ID") "STK_ID", "TRADE_DT", "CLS_PRC"
                    FROM "TB_PRC_DAY"
                    WHERE "MKT_CD" = :marketCode
                      AND (CAST(:stockCode AS VARCHAR) IS NULL OR "STK_CD" = CAST(:stockCode AS VARCHAR))
                    ORDER BY "STK_ID", "TRADE_DT" DESC
                )
                UPDATE "TB_HOLD" h
                SET "CUR_PRC" = p."CLS_PRC",
                    "PRC_BASE_DT" = p."TRADE_DT",
                    "ORG_EVL_AMT" = h."HOLD_QTY" * p."CLS_PRC",
                    "EVL_AMT" = h."HOLD_QTY" * p."CLS_PRC" * h."EXCH_RT",
                    "ORG_PL_AMT" = (p."CLS_PRC" - h."AVG_PRC") * h."HOLD_QTY",
                    "PL_AMT" = (p."CLS_PRC" - h."AVG_PRC") * h."HOLD_QTY" * h."EXCH_RT",
                    "PL_RT" = CASE WHEN h."AVG_PRC" = 0 THEN NULL
                                   ELSE (p."CLS_PRC" - h."AVG_PRC") * 100 / h."AVG_PRC" END,
                    "CALC_DTTM" = CURRENT_TIMESTAMP
                FROM latest_price p
                WHERE h."STK_ID" = p."STK_ID"
                  AND h."USE_YN" = 'Y'
                  AND h."DEL_YN" = 'N'
                  AND (h."PRC_BASE_DT" IS NULL OR h."PRC_BASE_DT" <= p."TRADE_DT")
                """)
        .param("marketCode", marketCode)
        .param("stockCode", stockCode)
        .update();
  }
}
