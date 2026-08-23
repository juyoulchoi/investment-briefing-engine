package com.nanum.investment.marketdata.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/krx")
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "KRX 데이터",
    description = "KRX 원본 데이터 및 Collection Job API")
public class KrxCollectionJobListController {
  private final JdbcClient jdbc;

  public KrxCollectionJobListController(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/collection-jobs")
  @io.swagger.v3.oas.annotations.Operation(summary = "KRX Collection Job 목록 조회")
  public List<Map<String, Object>> findAll(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate baseDate,
      @RequestParam(defaultValue = "20") int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 100));
    return jdbc.sql(
            """
                SELECT j.id AS job_id, j.base_date, j.status,
                  CASE
                    WHEN EXISTS (
                      SELECT 1 FROM tb_krx_clct_job_item i
                      WHERE i.job_id=j.id
                        AND i.dataset_code LIKE '%_DAILY'
                        AND i.received_count=0
                    ) THEN 'NO_DAILY_DATA'
                    ELSE 'DATA_RECEIVED'
                  END AS data_status,
                  j.total_count, j.success_count, j.failed_count,
                  COALESCE((
                    SELECT sum(i.received_count)
                    FROM tb_krx_clct_job_item i WHERE i.job_id=j.id
                  ), 0) AS received_count,
                  j.created_at, j.started_at, j.completed_at
                FROM tb_krx_clct_job j
                WHERE (CAST(:baseDate AS date) IS NULL OR j.base_date=CAST(:baseDate AS date))
                ORDER BY j.created_at DESC
                LIMIT :limit
                """)
        .param("baseDate", baseDate)
        .param("limit", safeLimit)
        .query()
        .listOfRows();
  }
}
