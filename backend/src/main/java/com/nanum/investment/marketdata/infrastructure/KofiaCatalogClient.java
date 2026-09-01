package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KofiaCatalogClient {
  private static final String DIVISIONS =
      "MSIS10000000000000,MSIS20000000000000,MSIS30000000000000,MSIS35000000000000,"
          + "MSIS40000000000000,MSIS60000000000000,MSIS70000000000000,MSIS50000000000000,"
          + "MSIS80000000000000,MSIS90000000000000,MSIS02000000000000,MSIS04000000000000,"
          + "MSIS06000000000000,MSIS95000000000000,MSIS07000000000000";

  private final RestClient client;

  public KofiaCatalogClient(
      @Value("${kofia.base-url}") String baseUrl,
      @Value("${kofia.connect-timeout:5s}") Duration connectTimeout,
      @Value("${kofia.read-timeout:30s}") Duration readTimeout) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(connectTimeout)
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(readTimeout);
    client =
        RestClient.builder()
            .requestFactory(factory)
            .baseUrl(baseUrl)
            .defaultHeader("Accept", "application/json")
            .build();
  }

  public JsonNode favorites() {
    Map<String, Object> data = new LinkedHashMap<>();
    for (String key :
        new String[] {
          "userId", "serviceId", "tmpV1", "tmpV45", "tmpV46", "tmpV108", "sqlKey",
          "searchLog", "ipAddress"
        }) data.put(key, "");
    JsonNode response = post("/app/favorites/STATCOMFAVORITESTATBO.do", Map.of("data", data));
    if (!response.path("success").asBoolean() || !response.path("dsResultList").isArray())
      throw new IllegalStateException("KOFIA 즐겨찾는 통계 목록 응답이 올바르지 않습니다.");
    return response;
  }

  public JsonNode metadata(String serviceId) {
    Map<String, Object> search =
        Map.of(
            "strSvrId", serviceId,
            "strDivId", DIVISIONS,
            "app_peron_yn", "Y",
            "language_gb", "KOR",
            "strGetCode", "Y");
    JsonNode response = post("/meta/getSrvData.do", Map.of("dmSearchData", search));
    if (!response.path("dsGridSQL").isArray() && !response.path("dsGridServlet").isArray())
      throw new IllegalStateException("KOFIA 서비스 메타데이터 응답이 올바르지 않습니다: " + serviceId);
    return response;
  }

  private JsonNode post(String path, Object body) {
    return client
        .post()
        .uri(path)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(JsonNode.class);
  }
}
