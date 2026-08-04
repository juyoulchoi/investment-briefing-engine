package com.nanum.investment.service;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.LocalDate;
import java.util.List;

@Service
public class AdditionalBuyViewService {
 private final JdbcClient jdbc;
 public AdditionalBuyViewService(JdbcClient jdbc){this.jdbc=jdbc;}
 public AdditionalBuyViewResult latest(){
  LocalDate date=jdbc.sql("SELECT max(\"BASE_DT\") FROM \"TB_ADD_BUY\"").query(LocalDate.class).optional().orElse(null);
  BigDecimal reserve=jdbc.sql("SELECT COALESCE(sum(\"RSV_AMT\"),0) FROM \"TB_CASH_RSV\"").query(BigDecimal.class).single();
  if(date==null)return new AdditionalBuyViewResult(null,reserve,BigDecimal.ZERO,BigDecimal.ZERO,List.of());
  List<AdditionalBuyViewResult.Candidate> candidates=jdbc.sql("""
   SELECT b."ADD_BUY_ID",b."ACCT_ID",a."ACCT_TP",b."STK_ID",s."STK_CD",s."STK_NM",b."PRIO_NO",b."PRIO_SCR",
    COALESCE(b."RCMD_ADD_AMT",0),b."ELIG_RSN",b."EXEC_YN"
   FROM "TB_ADD_BUY" b JOIN "TB_ACCT" a ON a."ACCT_ID"=b."ACCT_ID" JOIN "TB_STK" s ON s."STK_ID"=b."STK_ID"
   WHERE b."BASE_DT"=:day AND b."ELIG_YN"='Y' AND COALESCE(b."RCMD_ADD_AMT",0)>0
   ORDER BY b."PRIO_NO" NULLS LAST,b."PRIO_SCR" DESC,s."STK_CD"
   """).param("day",date).query((rs,n)->new AdditionalBuyViewResult.Candidate(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getLong(4),rs.getString(5),rs.getString(6),rs.getObject(7,Integer.class),rs.getBigDecimal(8),rs.getBigDecimal(9),rs.getString(10),rs.getString(11))).list();
  BigDecimal total=candidates.stream().map(AdditionalBuyViewResult.Candidate::recommendedAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
  BigDecimal usage=reserve.signum()==0?BigDecimal.ZERO:total.multiply(new BigDecimal("100")).divide(reserve,2,RoundingMode.HALF_UP);
  return new AdditionalBuyViewResult(date,reserve,total,usage,List.copyOf(candidates));
 }
}
