package com.nanum.investment.holding.application;

import com.nanum.investment.common.domain.TbAcct;
import com.nanum.investment.holding.domain.TbHold;
import com.nanum.investment.holding.infrastructure.repository.TbHoldRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class TargetWeightRefreshService {
  private static final int HOLDING_SCALE = 4;
  private static final int SETTING_SCALE = 6;

  private final TbHoldRepository holdings;
  private final JdbcClient jdbc;

  public TargetWeightRefreshService(TbHoldRepository holdings, JdbcClient jdbc) {
    this.holdings = holdings;
    this.jdbc = jdbc;
  }

  public void refreshAccount(TbAcct account) {
    List<TbHold> accountHoldings =
        holdings.findAllByAccount_AccountIdAndDeleteYn(account.getAccountId(), "N");
    Map<Long, Integer> scores = scores(account.getAccountId());
    int totalScore =
        accountHoldings.stream()
            .filter(TargetWeightRefreshService::isActive)
            .map(TbHold::getStock)
            .map(stock -> scores.get(stock.getStockId()))
            .filter(score -> score != null && score > 0)
            .mapToInt(Integer::intValue)
            .sum();

    for (TbHold holding : accountHoldings) {
      Integer score = scores.get(holding.getStock().getStockId());
      BigDecimal settingTarget = target(score, totalScore, isActive(holding), SETTING_SCALE);
      BigDecimal holdingTarget =
          settingTarget == null
              ? null
              : settingTarget.multiply(BigDecimal.valueOf(100)).setScale(HOLDING_SCALE);
      holding.setTargetWeight(holdingTarget);
      upsertSetting(account, holding, score, settingTarget);
    }
  }

  static BigDecimal target(Integer score, int totalScore, boolean active, int scale) {
    if (!active) return BigDecimal.ZERO.setScale(scale);
    if (score == null || score <= 0 || totalScore <= 0) return null;
    return BigDecimal.valueOf(score)
        .divide(BigDecimal.valueOf(totalScore), scale, RoundingMode.HALF_UP);
  }

  private Map<Long, Integer> scores(Long accountId) {
    Map<Long, Integer> scores = new HashMap<>();
    jdbc.sql(
            """
      SELECT h."STK_ID",CAST(c."CD_KEY" AS INTEGER)
      FROM "TB_HOLD" h
      LEFT JOIN "TB_REG_BUY" r
        ON r."ACCT_ID"=h."ACCT_ID" AND r."STK_ID"=h."STK_ID" AND r."DEL_YN"='N'
      LEFT JOIN "TB_CD_DTL" c
        ON c."CD_GRP"='INVESTMENT_GRADE' AND c."CD_NM"=r."INV_GRD" AND c."ACTV_YN"='Y'
      WHERE h."ACCT_ID"=:accountId AND h."DEL_YN"='N'
      """)
        .param("accountId", accountId)
        .query(
            (rs, rowNum) -> {
              Integer score = rs.getObject(2, Integer.class);
              return new ScoreRow(rs.getLong(1), score);
            })
        .list()
        .forEach(row -> scores.put(row.stockId(), row.score()));
    return scores;
  }

  private void upsertSetting(
      TbAcct account, TbHold holding, Integer score, BigDecimal settingTarget) {
    jdbc.sql(
            """
      INSERT INTO "TB_STK_SET"(
        "ACCT_TP","MKT_CD","STK_CD","WGT_SCR","TGT_WGT","REG_DT","MOD_DT")
      VALUES(:accountType,:marketCode,:stockCode,:score,:target,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
      ON CONFLICT ("ACCT_TP","STK_CD") DO UPDATE
      SET "MKT_CD"=EXCLUDED."MKT_CD",
          "WGT_SCR"=EXCLUDED."WGT_SCR",
          "TGT_WGT"=EXCLUDED."TGT_WGT",
          "MOD_DT"=CURRENT_TIMESTAMP
      """)
        .param("accountType", account.getAccountType().name())
        .param("marketCode", holding.getStock().getMarketCode())
        .param("stockCode", holding.getStock().getStockCode())
        .param("score", score, Types.INTEGER)
        .param("target", settingTarget, Types.NUMERIC)
        .update();
  }

  private static boolean isActive(TbHold holding) {
    return "Y".equals(holding.getUseYn());
  }

  private record ScoreRow(Long stockId, Integer score) {}
}
