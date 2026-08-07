package com.nanum.investment.api.v1;

import com.nanum.investment.common.response.ApiResponse;
import com.nanum.investment.common.web.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/briefings")
public class BriefingHistoryController {
    private final JdbcClient jdbc;

    public BriefingHistoryController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record HistoryRow(Long briefingId, LocalDate baseDate, String briefingType,
            String title, String summary, String status, String publishedYn,
            BigDecimal confidenceRate, BigDecimal marketScore, String marketRegime) {}

    @GetMapping("/history")
    public ApiResponse<List<HistoryRow>> history(@RequestParam(defaultValue = "DAILY") String type,
            HttpServletRequest request) {
        String briefingType = switch (type.toUpperCase()) {
            case "DAILY", "WEEKLY", "MONTHLY" -> type.toUpperCase();
            default -> "DAILY";
        };
        List<HistoryRow> rows = jdbc.sql("""
                SELECT b."BRF_ID",b."BASE_DT",b."BRF_TP",b."TITLE",b."SUMMARY_TXT",
                       b."BRF_STS",b."PUBL_YN",b."CONF_RT",d."MKT_SCR",d."MKT_REGIME"
                  FROM "TB_BRF" b
                  LEFT JOIN "TB_INV_DEC" d ON d."INV_DEC_ID"=b."INV_DEC_ID"
                 WHERE b."BRF_TP"=:type AND b."SCOPE_TP"='GLOBAL' AND b."LATEST_YN"='Y'
                 ORDER BY b."BASE_DT" DESC,b."CALC_SEQ" DESC,b."BRF_ID" DESC
                """).param("type", briefingType).query((rs, rowNum) -> new HistoryRow(
                        rs.getLong("BRF_ID"), rs.getObject("BASE_DT", LocalDate.class),
                        rs.getString("BRF_TP"), rs.getString("TITLE"), rs.getString("SUMMARY_TXT"),
                        rs.getString("BRF_STS"), rs.getString("PUBL_YN"), rs.getBigDecimal("CONF_RT"),
                        rs.getBigDecimal("MKT_SCR"), rs.getString("MKT_REGIME"))).list();
        return ApiResponse.success(rows, TraceIdUtils.resolve(request));
    }
}
