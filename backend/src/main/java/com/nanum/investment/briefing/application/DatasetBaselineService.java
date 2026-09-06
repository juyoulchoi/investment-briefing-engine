package com.nanum.investment.briefing.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class DatasetBaselineService {
  private final JdbcClient jdbc;

  public DatasetBaselineService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public DatasetBaseline baseline(String version) {
    DatasetVersion header =
        jdbc.sql(
                """
                SELECT "DATASET_VER","DATASET_STS","FROZEN_DTTM","DESCRIPTION","CONTRACT_JSON"::text
                  FROM "TB_DATASET_VER" WHERE "DATASET_VER"=:version
                """)
            .param("version", version)
            .query(
                (rs, n) ->
                    new DatasetVersion(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getObject(3, OffsetDateTime.class),
                        rs.getString(4),
                        rs.getString(5)))
            .optional()
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 데이터셋 버전입니다: " + version));
    List<DatasetItem> items =
        jdbc.sql(
                """
                SELECT "ITEM_CD","ITEM_CLASS","PROVIDER_CD","STORAGE_NAME","AVAILABLE_YN","USED_BY_ALGORITHM_YN",
                       "MIN_BASE_DT","MAX_BASE_DT","ROW_CNT","REASON_TXT"
                  FROM "TB_DATASET_ITEM" WHERE "DATASET_VER"=:version
                 ORDER BY CASE "ITEM_CLASS" WHEN 'CORE' THEN 1 WHEN 'SUPPLEMENT' THEN 2 ELSE 3 END,"ITEM_CD"
                """)
            .param("version", version)
            .query(
                (rs, n) ->
                    new DatasetItem(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        "Y".equals(rs.getString(5)),
                        "Y".equals(rs.getString(6)),
                        rs.getObject(7, LocalDate.class),
                        rs.getObject(8, LocalDate.class),
                        (Long) rs.getObject(9),
                        rs.getString(10)))
            .list();
    return new DatasetBaseline(header, items);
  }

  public record DatasetBaseline(DatasetVersion version, List<DatasetItem> items) {}

  public record DatasetVersion(
      String datasetVersion,
      String status,
      OffsetDateTime frozenAt,
      String description,
      String contractJson) {}

  public record DatasetItem(
      String itemCode,
      String itemClass,
      String providerCode,
      String storageName,
      boolean available,
      boolean usedByAlgorithm,
      LocalDate minBaseDate,
      LocalDate maxBaseDate,
      Long rowCount,
      String reason) {}
}
