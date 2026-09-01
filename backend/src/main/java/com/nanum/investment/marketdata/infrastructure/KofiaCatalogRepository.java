package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class KofiaCatalogRepository {
  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;

  public KofiaCatalogRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void save(JsonNode favorite, JsonNode metadata, boolean normalize) {
    JsonNode sql = metadata.path("dsGridSQL").path(0);
    JsonNode servlet = metadata.path("dsGridServlet").path(0);
    JsonNode info = metadata.path("dsGridInfo").path(0);
    String serviceId = favorite.path("TMPV2").asText();
    String metaHash = KofiaSupport.sha256(metadata.toString());
    String objectName = sql.path("OBJ_NM").asText(servlet.path("OBJ_NM").asText(null));
    jdbc.sql(
            """
        INSERT INTO "TB_KOFIA_SVC"("SERVICE_ID","SERVICE_NM","MENU_PATH","PARENT_DIV_ID",
          "MENU_DIV_ID","OBJ_NM","OUTPUT_FORM_NO","OUTPUT_SQL_KEY","POPULARITY_VAL",
          "FAVORITE_ROW_SEQ","HEADER_META","SEARCH_META","UNIT_META","LATEST_DATE_META",
          "META_HASH","NORMALIZE_YN")
        VALUES(:id,:name,:path,:parent,:menu,:object,:form,:sqlKey,:popularity,:seq,
          CAST(:headers AS jsonb),CAST(:search AS jsonb),CAST(:unit AS jsonb),CAST(:latest AS jsonb),
          :hash,:normalize)
        ON CONFLICT("SERVICE_ID") DO UPDATE SET
          "SERVICE_NM"=EXCLUDED."SERVICE_NM","MENU_PATH"=EXCLUDED."MENU_PATH",
          "PARENT_DIV_ID"=EXCLUDED."PARENT_DIV_ID","MENU_DIV_ID"=EXCLUDED."MENU_DIV_ID",
          "OBJ_NM"=EXCLUDED."OBJ_NM","OUTPUT_FORM_NO"=EXCLUDED."OUTPUT_FORM_NO",
          "OUTPUT_SQL_KEY"=EXCLUDED."OUTPUT_SQL_KEY","POPULARITY_VAL"=EXCLUDED."POPULARITY_VAL",
          "FAVORITE_ROW_SEQ"=EXCLUDED."FAVORITE_ROW_SEQ","HEADER_META"=EXCLUDED."HEADER_META",
          "SEARCH_META"=EXCLUDED."SEARCH_META","UNIT_META"=EXCLUDED."UNIT_META",
          "LATEST_DATE_META"=EXCLUDED."LATEST_DATE_META","META_HASH"=EXCLUDED."META_HASH",
          "NORMALIZE_YN"=EXCLUDED."NORMALIZE_YN","USE_YN"='Y',
          "LAST_DISCOVER_DTTM"=CURRENT_TIMESTAMP,"UPD_DTTM"=CURRENT_TIMESTAMP
        """)
        .param("id", serviceId)
        .param("name", favorite.path("TMPV6").asText(sql.path("SERVICE_NM").asText(serviceId)))
        .param("path", favorite.path("TMPV5").asText(null))
        .param("parent", favorite.path("TMPV3").asText(null))
        .param("menu", favorite.path("TMPV4").asText(null))
        .param("object", objectName)
        .param("form", sql.path("OUTPUT_FORMNO").asText(info.path("OUTPUT_FORMNO").asText(null)))
        .param("sqlKey", sql.path("OUTPUT_SQLKEY").asText(null))
        .param("popularity", favorite.path("TMPV7").isNumber() ? favorite.path("TMPV7").longValue() : null)
        .param("seq", favorite.path("ROWSEQ").asInt())
        .param("headers", metadata.path("dsGrid").toString())
        .param("search", metadata.path("dsSearch").toString())
        .param("unit", json(Map.of(
            "basicUnit", info.path("BASIC_UNIT").asText(""),
            "display", info.path("BASIC_UNITDSP").asText(""))))
        .param("latest", metadata.path("dsLatestDate").toString())
        .param("hash", metaHash)
        .param("normalize", normalize ? "Y" : "N")
        .update();
    jdbc.sql(
            """
        INSERT INTO "TB_KOFIA_SVC_RANK_HIS"("SNAPSHOT_DT","SERVICE_ID","ROW_SEQ","POPULARITY_VAL")
        VALUES(:day,:id,:seq,:popularity)
        ON CONFLICT("SNAPSHOT_DT","SERVICE_ID") DO UPDATE SET
          "ROW_SEQ"=EXCLUDED."ROW_SEQ","POPULARITY_VAL"=EXCLUDED."POPULARITY_VAL",
          "COLLECT_DTTM"=CURRENT_TIMESTAMP
        """)
        .param("day", LocalDate.now(java.time.ZoneId.of("Asia/Seoul")))
        .param("id", serviceId)
        .param("seq", favorite.path("ROWSEQ").asInt())
        .param("popularity", favorite.path("TMPV7").isNumber() ? favorite.path("TMPV7").longValue() : null)
        .update();
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception error) {
      throw new IllegalStateException("KOFIA 카탈로그 JSON 변환에 실패했습니다.", error);
    }
  }

  public List<Map<String, Object>> services() {
    return jdbc.sql(
            """
        SELECT "SERVICE_ID" service_id,"SERVICE_NM" service_name,"MENU_PATH" menu_path,
          "OBJ_NM" object_name,"POPULARITY_VAL" popularity_value,
          "FAVORITE_ROW_SEQ" favorite_row_sequence,"NORMALIZE_YN" normalize_yn,
          "META_HASH" metadata_hash,"LAST_DISCOVER_DTTM" last_discovered_at
        FROM "TB_KOFIA_SVC" WHERE "USE_YN"='Y' ORDER BY "FAVORITE_ROW_SEQ","SERVICE_ID"
        """)
        .query()
        .listOfRows();
  }
}
