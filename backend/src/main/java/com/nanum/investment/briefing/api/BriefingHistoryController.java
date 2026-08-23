package com.nanum.investment.briefing.api;

import com.nanum.investment.common.response.ApiResponse;
import com.nanum.investment.common.web.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/briefings")
@io.swagger.v3.oas.annotations.tags.Tag(name = "브리핑", description = "투자 브리핑 조회 및 생성 API")
public class BriefingHistoryController {
  private final JdbcClient jdbc;

  public BriefingHistoryController(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public record HistoryRow(
      Long briefingId,
      LocalDate baseDate,
      String briefingType,
      String title,
      String summary,
      String status,
      String publishedYn,
      BigDecimal confidenceRate,
      BigDecimal marketScore,
      String marketRegime) {}

  public record DetailItem(String itemCode, String summary, String content, String signalCode) {}

  public record BriefingDetail(
      Long briefingId,
      LocalDate baseDate,
      String briefingType,
      String title,
      String summary,
      String body,
      String status,
      String publishedYn,
      BigDecimal confidenceRate,
      List<DetailItem> items) {}

  private record DetailHeader(
      Long briefingId,
      LocalDate baseDate,
      String briefingType,
      String title,
      String summary,
      String body,
      String status,
      String publishedYn,
      BigDecimal confidenceRate) {}

  @GetMapping("/history")
  @io.swagger.v3.oas.annotations.Operation(summary = "브리핑 이력 조회")
  public ApiResponse<List<HistoryRow>> history(
      @RequestParam(defaultValue = "DAILY") String type, HttpServletRequest request) {
    String briefingType =
        switch (type.toUpperCase()) {
          case "DAILY", "WEEKLY", "MONTHLY" -> type.toUpperCase();
          default -> "DAILY";
        };
    List<HistoryRow> rows =
        jdbc.sql(
                """
                SELECT b."BRF_ID",b."BASE_DT",b."BRF_TP",b."TITLE",b."SUMMARY_TXT",
                       b."BRF_STS",b."PUBL_YN",b."CONF_RT",d."MKT_SCR",d."MKT_REGIME"
                  FROM "TB_BRF" b
                  LEFT JOIN "TB_INV_DEC" d ON d."INV_DEC_ID"=b."INV_DEC_ID"
                 WHERE b."BRF_TP"=:type AND b."SCOPE_TP"='GLOBAL' AND b."LATEST_YN"='Y'
                 ORDER BY b."BASE_DT" DESC,b."CALC_SEQ" DESC,b."BRF_ID" DESC
                """)
            .param("type", briefingType)
            .query(
                (rs, rowNum) ->
                    new HistoryRow(
                        rs.getLong("BRF_ID"),
                        rs.getObject("BASE_DT", LocalDate.class),
                        rs.getString("BRF_TP"),
                        rs.getString("TITLE"),
                        rs.getString("SUMMARY_TXT"),
                        rs.getString("BRF_STS"),
                        rs.getString("PUBL_YN"),
                        rs.getBigDecimal("CONF_RT"),
                        rs.getBigDecimal("MKT_SCR"),
                        rs.getString("MKT_REGIME")))
            .list();
    return ApiResponse.success(rows, TraceIdUtils.resolve(request));
  }

  @GetMapping("/{id}")
  @io.swagger.v3.oas.annotations.Operation(summary = "브리핑 상세 조회")
  public ApiResponse<BriefingDetail> detail(@PathVariable Long id, HttpServletRequest request) {
    DetailHeader header =
        jdbc.sql(
                """
                SELECT "BRF_ID","BASE_DT","BRF_TP","TITLE","SUMMARY_TXT","BODY_TXT",
                       "BRF_STS","PUBL_YN","CONF_RT"
                  FROM "TB_BRF" WHERE "BRF_ID"=:id
                """)
            .param("id", id)
            .query(
                (rs, rowNum) ->
                    new DetailHeader(
                        rs.getLong("BRF_ID"),
                        rs.getObject("BASE_DT", LocalDate.class),
                        rs.getString("BRF_TP"),
                        rs.getString("TITLE"),
                        rs.getString("SUMMARY_TXT"),
                        rs.getString("BODY_TXT"),
                        rs.getString("BRF_STS"),
                        rs.getString("PUBL_YN"),
                        rs.getBigDecimal("CONF_RT")))
            .optional()
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "브리핑을 찾을 수 없습니다."));
    List<DetailItem> items =
        jdbc.sql(
                """
                SELECT "ITEM_CD","ITEM_SUM","ITEM_CONT","SIG_CD"
                  FROM "TB_BRF_DTL" WHERE "BRF_ID"=:id ORDER BY "DTL_ID"
                """)
            .param("id", id)
            .query(
                (rs, rowNum) ->
                    new DetailItem(
                        rs.getString("ITEM_CD"), rs.getString("ITEM_SUM"),
                        rs.getString("ITEM_CONT"), rs.getString("SIG_CD")))
            .list();
    return ApiResponse.success(
        new BriefingDetail(
            header.briefingId(),
            header.baseDate(),
            header.briefingType(),
            header.title(),
            header.summary(),
            header.body(),
            header.status(),
            header.publishedYn(),
            header.confidenceRate(),
            items),
        TraceIdUtils.resolve(request));
  }
}
