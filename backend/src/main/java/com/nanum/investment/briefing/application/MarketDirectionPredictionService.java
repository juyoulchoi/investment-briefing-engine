package com.nanum.investment.briefing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.briefing.dto.request.MarketDirectionDto;
import com.nanum.investment.briefing.dto.request.MarketScenarioProbabilityDto;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketDirectionPredictionService {
  private final JdbcClient jdbc;
  private final ObjectMapper json;
  public MarketDirectionPredictionService(JdbcClient jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }

  @Transactional
  public MarketDirectionDto calculateAndSave(LocalDate date) {
    DirectionInput input = jdbc.sql("""
        SELECT round("MKT_SCR")::int, round("RISK_SCR")::int, "MKT_REGIME",
               (SELECT max("BASE_DT") FROM "TB_MKT_SNAP" WHERE "BASE_DT"<=:day),
               (SELECT max("BASE_DT") FROM "TB_EXCH_DAY" WHERE "BASE_DT"<=:day),
               (SELECT max("BASE_DT") FROM "TB_FRED_BOND_DAY" WHERE "BASE_DT"<=:day)
        FROM "TB_INV_DEC" WHERE "BASE_DT"=:day AND "LATEST_YN"='Y'
        ORDER BY "CALC_SEQ" DESC LIMIT 1
        """).param("day", date).query((rs,n) -> new DirectionInput(
            rs.getInt(1), rs.getInt(2), rs.getString(3),
            rs.getObject(4, LocalDate.class), rs.getObject(5, LocalDate.class), rs.getObject(6, LocalDate.class)))
        .optional().orElseThrow(() -> new IllegalStateException("시장방향 예측에 필요한 투자판단이 없습니다."));
    int score = clamp((input.marketScore() + (100 - input.riskScore())) / 2);
    int[] probabilities = normalize(
        10 + score * 35 / 100,
        45 - Math.abs(score - 50) / 3,
        10 + (100 - score) * 25 / 100,
        5 + (100 - score) * 20 / 100);
    Previous previous = jdbc.sql("""
        SELECT "UPTREND_RESUME_PROB","BOX_RANGE_PROB","RE_CORRECTION_PROB","RETEST_LOW_PROB"
        FROM "TB_MKT_DIR_PRED" WHERE "BASE_DT"<:day AND "LATEST_YN"='Y' ORDER BY "BASE_DT" DESC LIMIT 1
        """).param("day", date).query((rs,n) -> new Previous(rs.getInt(1),rs.getInt(2),rs.getInt(3),rs.getInt(4)))
        .optional().orElse(new Previous(probabilities[0], probabilities[1], probabilities[2], probabilities[3]));
    int sequence = jdbc.sql("SELECT COALESCE(max(\"CALC_SEQ\"),0)+1 FROM \"TB_MKT_DIR_PRED\" WHERE \"BASE_DT\"=:day")
        .param("day", date).query(Integer.class).single();
    jdbc.sql("UPDATE \"TB_MKT_DIR_PRED\" SET \"LATEST_YN\"='N' WHERE \"BASE_DT\"=:day AND \"LATEST_YN\"='Y'").param("day", date).update();
    Map<String,Object> dates = new LinkedHashMap<>();
    dates.put("investmentDecision", date); dates.put("marketSnapshot", input.snapshotDate());
    dates.put("exchangeRate", input.exchangeDate()); dates.put("bondYield", input.bondDate());
    Map<String,Object> basis = Map.of("marketScore", input.marketScore(), "riskScore", input.riskScore(), "marketPhase", input.marketPhase());
    jdbc.sql("""
        INSERT INTO "TB_MKT_DIR_PRED"("BASE_DT","CALC_SEQ","DIR_SCR","UPTREND_RESUME_PROB","BOX_RANGE_PROB",
          "RE_CORRECTION_PROB","RETEST_LOW_PROB","UPTREND_RESUME_CHG","BOX_RANGE_CHG","RE_CORRECTION_CHG",
          "RETEST_LOW_CHG","INPUT_BASE_DT_JSON","CALC_BASIS_JSON","LATEST_YN")
        VALUES(:day,:seq,:score,:up,:box,:correction,:low,:upChange,:boxChange,:correctionChange,:lowChange,
          CAST(:dates AS jsonb),CAST(:basis AS jsonb),'Y')
        """).param("day",date).param("seq",sequence).param("score",score)
        .param("up",probabilities[0]).param("box",probabilities[1]).param("correction",probabilities[2]).param("low",probabilities[3])
        .param("upChange",probabilities[0]-previous.up()).param("boxChange",probabilities[1]-previous.box())
        .param("correctionChange",probabilities[2]-previous.correction()).param("lowChange",probabilities[3]-previous.low())
        .param("dates",toJson(dates)).param("basis",toJson(basis)).update();
    return new MarketDirectionDto(score, new MarketScenarioProbabilityDto(
        probabilities[0], probabilities[1], probabilities[2], probabilities[3],
        probabilities[0]-previous.up(), probabilities[1]-previous.box(), probabilities[2]-previous.correction(), probabilities[3]-previous.low()));
  }

  static int[] normalize(int... weights) {
    int sum = Arrays.stream(weights).sum(), assigned = 0;
    int[] result = new int[weights.length];
    for (int i=0;i<weights.length-1;i++) { result[i] = weights[i] * 100 / sum; assigned += result[i]; }
    result[result.length-1] = 100-assigned;
    return result;
  }
  public static void validate(MarketScenarioProbabilityDto value) {
    int[] values = {value.uptrendResume(),value.boxRange(),value.reCorrection(),value.retestLow()};
    if (Arrays.stream(values).anyMatch(v -> v < 0 || v > 100) || Arrays.stream(values).sum() != 100)
      throw new IllegalStateException("시장방향 시나리오 확률은 각각 0~100이며 합계가 100이어야 합니다.");
  }
  private int clamp(int v) { return Math.max(0, Math.min(100, v)); }
  private String toJson(Object value) { try { return json.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalStateException(e); } }
  private record DirectionInput(int marketScore,int riskScore,String marketPhase,LocalDate snapshotDate,LocalDate exchangeDate,LocalDate bondDate) {}
  private record Previous(int up,int box,int correction,int low) {}
}
