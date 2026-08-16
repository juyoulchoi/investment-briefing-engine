package com.nanum.investment.regularbuy.application;

import java.math.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class AdditionalBuyViewService {
  private final JdbcClient jdbc;

  public AdditionalBuyViewService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  private record LatestDecision(Long id, LocalDate date) {}

  public AdditionalBuyViewResult latest() {
    LatestDecision latest =
        jdbc.sql(
                "SELECT d.\"INV_DEC_ID\",d.\"BASE_DT\" FROM \"TB_INV_DEC\" d WHERE d.\"LATEST_YN\"='Y' AND d.\"DATA_STS\" IN ('FRESH','PARTIAL') AND EXISTS (SELECT 1 FROM \"TB_STK_DEC\" sd JOIN \"TB_ADD_BUY\" b ON b.\"STK_DEC_ID\"=sd.\"STK_DEC_ID\" WHERE sd.\"INV_DEC_ID\"=d.\"INV_DEC_ID\") ORDER BY d.\"BASE_DT\" DESC,d.\"CALC_SEQ\" DESC LIMIT 1")
            .query((rs, n) -> new LatestDecision(rs.getLong(1), rs.getObject(2, LocalDate.class)))
            .optional()
            .orElse(null);
    LocalDate date = latest == null ? null : latest.date();
    BigDecimal reserve =
        jdbc.sql("SELECT COALESCE(sum(\"RSV_AMT\"),0) FROM \"TB_CASH_RSV\"")
            .query(BigDecimal.class)
            .single();
    if (date == null)
      return new AdditionalBuyViewResult(
          null, reserve, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    List<AdditionalBuyViewResult.Candidate> candidates =
        jdbc.sql(
                """
   SELECT b."ADD_BUY_ID",b."ACCT_ID",a."ACCT_TP",b."STK_ID",s."STK_CD",s."STK_NM",b."ELIG_YN",r."PRIORITY",b."PRIO_SCR",
    COALESCE(b."RCMD_ADD_AMT",0),b."ELIG_RSN",b."EXEC_YN"
   FROM "TB_ADD_BUY" b JOIN "TB_STK_DEC" sd ON sd."STK_DEC_ID"=b."STK_DEC_ID" JOIN "TB_ACCT" a ON a."ACCT_ID"=b."ACCT_ID" JOIN "TB_STK" s ON s."STK_ID"=b."STK_ID" LEFT JOIN "TB_REG_BUY" r ON r."ACCT_ID"=b."ACCT_ID" AND r."STK_ID"=b."STK_ID" AND r."DEL_YN"='N'
   WHERE b."BASE_DT"=:day AND sd."INV_DEC_ID"=:decisionId
   ORDER BY CASE WHEN b."ELIG_YN"='Y' THEN 0 ELSE 1 END,r."PRIORITY" DESC NULLS LAST,b."PRIO_SCR" DESC,s."STK_CD"
   """)
            .param("day", date)
            .param("decisionId", latest.id())
            .query(
                (rs, n) ->
                    new AdditionalBuyViewResult.Candidate(
                        rs.getLong(1),
                        rs.getLong(2),
                        rs.getString(3),
                        rs.getLong(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        rs.getObject(8, Integer.class),
                        rs.getBigDecimal(9),
                        rs.getBigDecimal(10),
                        rs.getString(11),
                        rs.getString(12)))
            .list();
    BigDecimal total =
        candidates.stream()
            .map(AdditionalBuyViewResult.Candidate::recommendedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal usage =
        reserve.signum() == 0
            ? BigDecimal.ZERO
            : total.multiply(new BigDecimal("100")).divide(reserve, 2, RoundingMode.HALF_UP);
    return new AdditionalBuyViewResult(date, reserve, total, usage, List.copyOf(candidates));
  }
}
