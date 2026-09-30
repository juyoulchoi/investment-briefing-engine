package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class KofiaLookupRepository {
  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;

  public KofiaLookupRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public int saveBusinesses(String marketType, JsonNode response) {
    Map<String, Object> request =
        Map.of("dmBusinessSearch", Map.of("tmpV1", marketType, "tmpV2", ""));
    saveRaw("BUSINESS", marketType, request, response);
    jdbc.sql(
            "UPDATE \"TB_KOFIA_BUSINESS_REF\" SET \"USE_YN\"='N',\"UPD_DTTM\"=CURRENT_TIMESTAMP WHERE \"MARKET_TYP_CD\"=:market")
        .param("market", marketType)
        .update();
    int count = 0;
    for (JsonNode row : response.path("dsList")) {
      String hash = KofiaSupport.sha256(row.toString());
      jdbc.sql(
              """
          INSERT INTO "TB_KOFIA_BUSINESS_REF"("MARKET_TYP_CD","LARGE_CATEGORY_CD",
            "MIDDLE_CATEGORY_CD","SMALL_CATEGORY_CD","BUSINESS_SHORT_CD","BUSINESS_NM",
            "SORT_SEQ","PAYLOAD","RAW_HASH")
          VALUES(:market,:large,:middle,:small,:short,:name,:sequence,CAST(:payload AS jsonb),:hash)
          ON CONFLICT("MARKET_TYP_CD","LARGE_CATEGORY_CD","MIDDLE_CATEGORY_CD",
            "SMALL_CATEGORY_CD","BUSINESS_SHORT_CD") DO UPDATE SET
            "BUSINESS_NM"=EXCLUDED."BUSINESS_NM","SORT_SEQ"=EXCLUDED."SORT_SEQ",
            "PAYLOAD"=EXCLUDED."PAYLOAD","RAW_HASH"=EXCLUDED."RAW_HASH","USE_YN"='Y',
            "LAST_COLLECT_DTTM"=CURRENT_TIMESTAMP,"UPD_DTTM"=CASE
              WHEN "TB_KOFIA_BUSINESS_REF"."RAW_HASH"<>EXCLUDED."RAW_HASH"
              THEN CURRENT_TIMESTAMP ELSE "TB_KOFIA_BUSINESS_REF"."UPD_DTTM" END
          """)
          .param("market", marketType)
          .param("large", row.path("TMPV1").asText(""))
          .param("middle", row.path("TMPV2").asText(""))
          .param("small", row.path("TMPV3").asText(""))
          .param("short", row.path("TMPV4").asText(""))
          .param("name", row.path("TMPV5").asText(""))
          .param("sequence", row.path("TMPV6").isNumber() ? row.path("TMPV6").asInt() : null)
          .param("payload", row.toString())
          .param("hash", hash)
          .update();
      count++;
    }
    return count;
  }

  @Transactional
  public int saveCompanies(String tableName, String companyType, JsonNode response) {
    String canonicalTable = tableName.trim();
    Map<String, Object> request =
        Map.of(
            "dmCompanySearch", Map.of("searchNm", "", "tableNm", tableName, "tmpV18", companyType));
    saveRaw("COMPANY", canonicalTable + ":" + companyType, request, response);
    jdbc.sql(
            "UPDATE \"TB_KOFIA_COMPANY_REF\" SET \"USE_YN\"='N',\"UPD_DTTM\"=CURRENT_TIMESTAMP WHERE \"TABLE_NM\"=:table AND \"COMPANY_TYP_CD\"=:type")
        .param("table", canonicalTable)
        .param("type", companyType)
        .update();
    int count = 0;
    for (JsonNode row : response.path("dsList")) {
      String hash = KofiaSupport.sha256(row.toString());
      jdbc.sql(
              """
          INSERT INTO "TB_KOFIA_COMPANY_REF"("TABLE_NM","COMPANY_TYP_CD","COMPANY_CD",
            "COMPANY_NM","MIDDLE_CATEGORY_CD","PAYLOAD","RAW_HASH")
          VALUES(:table,:type,:code,:name,:middle,CAST(:payload AS jsonb),:hash)
          ON CONFLICT("TABLE_NM","COMPANY_TYP_CD","COMPANY_CD") DO UPDATE SET
            "COMPANY_NM"=EXCLUDED."COMPANY_NM","MIDDLE_CATEGORY_CD"=EXCLUDED."MIDDLE_CATEGORY_CD",
            "PAYLOAD"=EXCLUDED."PAYLOAD","RAW_HASH"=EXCLUDED."RAW_HASH","USE_YN"='Y',
            "LAST_COLLECT_DTTM"=CURRENT_TIMESTAMP,"UPD_DTTM"=CASE
              WHEN "TB_KOFIA_COMPANY_REF"."RAW_HASH"<>EXCLUDED."RAW_HASH"
              THEN CURRENT_TIMESTAMP ELSE "TB_KOFIA_COMPANY_REF"."UPD_DTTM" END
          """)
          .param("table", canonicalTable)
          .param("type", companyType)
          .param("code", row.path("TMPV2").asText(""))
          .param("name", row.path("TMPV1").asText(""))
          .param("middle", row.path("MIDDLECATEGORY").asText(""))
          .param("payload", row.toString())
          .param("hash", hash)
          .update();
      count++;
    }
    return count;
  }

  public List<Map<String, Object>> businesses(String marketType) {
    return jdbc.sql(
            """
        SELECT "MARKET_TYP_CD" market_type,"LARGE_CATEGORY_CD" large_category,
          "MIDDLE_CATEGORY_CD" middle_category,"SMALL_CATEGORY_CD" small_category,
          "BUSINESS_SHORT_CD" business_short_code,"BUSINESS_NM" business_name,
          "SORT_SEQ" sort_sequence,"USE_YN" use_yn,"LAST_COLLECT_DTTM" last_collected_at
        FROM "TB_KOFIA_BUSINESS_REF"
        WHERE "MARKET_TYP_CD"=:market AND "USE_YN"='Y'
        ORDER BY "SORT_SEQ","BUSINESS_NM"
        """)
        .param("market", marketType)
        .query()
        .listOfRows();
  }

  public List<Map<String, Object>> companies(String tableName, String companyType) {
    return jdbc.sql(
            """
        SELECT "TABLE_NM" table_name,"COMPANY_TYP_CD" company_type,"COMPANY_CD" company_code,
          "COMPANY_NM" company_name,"MIDDLE_CATEGORY_CD" middle_category,
          "USE_YN" use_yn,"LAST_COLLECT_DTTM" last_collected_at
        FROM "TB_KOFIA_COMPANY_REF"
        WHERE "TABLE_NM"=:table AND "COMPANY_TYP_CD"=:type AND "USE_YN"='Y'
        ORDER BY "COMPANY_NM","COMPANY_CD"
        """)
        .param("table", tableName.trim())
        .param("type", companyType)
        .query()
        .listOfRows();
  }

  private void saveRaw(
      String lookupType, String scopeCode, Map<String, Object> request, JsonNode response) {
    jdbc.sql(
            """
        INSERT INTO "TB_KOFIA_LOOKUP_RAW_RSP"("RAW_RSP_ID","LOOKUP_TYP","SCOPE_CD",
          "REQ_PARAMS","ROW_CNT","RAW_HASH","PAYLOAD")
        VALUES(:id,:type,:scope,CAST(:request AS jsonb),:count,:hash,CAST(:payload AS jsonb))
        """)
        .param("id", UUID.randomUUID())
        .param("type", lookupType)
        .param("scope", scopeCode)
        .param("request", json(request))
        .param("count", response.path("dsList").size())
        .param("hash", KofiaSupport.sha256(response.toString()))
        .param("payload", response.toString())
        .update();
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("KOFIA 검색 요청 파라미터 JSON 변환에 실패했습니다.", error);
    }
  }
}
