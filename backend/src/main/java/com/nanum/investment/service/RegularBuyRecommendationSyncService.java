package com.nanum.investment.service;

import java.time.LocalDate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegularBuyRecommendationSyncService {
  private final JdbcClient jdbc;

  public RegularBuyRecommendationSyncService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public int sync(LocalDate baseDate) {
    if (baseDate == null) throw new IllegalArgumentException("추천금액 계산 기준일이 필요합니다.");
    Long decisionId =
        jdbc.sql(
                """
                SELECT "INV_DEC_ID" FROM "TB_INV_DEC"
                WHERE "BASE_DT"=:day AND "ACCT_ID" IS NULL AND "MKT_SNAP_CD"='GLOBAL'
                  AND "LATEST_YN"='Y' AND "DATA_STS" IN ('FRESH','PARTIAL')
                ORDER BY "CALC_SEQ" DESC LIMIT 1
                """)
            .param("day", baseDate)
            .query(Long.class)
            .optional()
            .orElseThrow(() -> new IllegalStateException(baseDate + " 최신 투자판단이 없습니다."));
    return jdbc.sql(
            """
                UPDATE "TB_REG_BUY" r SET
                  "MKT_MULT"=d."MKT_MULT",
                  "FINAL_MULT"=d."FINAL_MULT",
                  "BASE_BUY_AMT"=d."BASE_BUY_AMT",
                  "RCMD_BUY_AMT"=d."REG_BUY_AMT",
                  "SAVED_AMT"=d."SAVED_AMT",
                  "ACT_SIG"=d."ACT_SIG",
                  "LAST_CALC_DT"=:day,
                  "RULE_VER_NO"=1
                FROM "TB_STK_DEC" d
                WHERE d."INV_DEC_ID"=:decisionId AND d."ACCT_ID"=r."ACCT_ID"
                  AND d."STK_ID"=r."STK_ID" AND r."DEL_YN"='N'
                """)
        .param("day", baseDate)
        .param("decisionId", decisionId)
        .update();
  }
}
