package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.common.infrastructure.external.CircuitBreakerSupport;
import com.nanum.investment.common.infrastructure.external.ExternalApiCallExecutor;
import com.nanum.investment.marketdata.domain.KofiaDataset;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KofiaRestClient implements KofiaClient {
  private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
  private final RestClient client;
  private final ExternalApiCallExecutor externalCalls;
  private final CircuitBreakerSupport circuitBreaker;
  private final String baseUrl;
  private final int failureThreshold;
  private final Duration openDuration;

  public KofiaRestClient(
      @Value("${kofia.base-url}") String baseUrl,
      @Value("${kofia.connect-timeout:5s}") Duration connectTimeout,
      @Value("${kofia.read-timeout:30s}") Duration readTimeout,
      @Value("${kofia.circuit-breaker.failure-threshold:5}") int failureThreshold,
      @Value("${kofia.circuit-breaker.open-duration:60s}") Duration openDuration,
      ExternalApiCallExecutor externalCalls,
      CircuitBreakerSupport circuitBreaker) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(connectTimeout)
            .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(readTimeout);
    this.client =
        RestClient.builder()
            .requestFactory(requestFactory)
            .baseUrl(baseUrl)
            .defaultHeader("Accept", "application/json")
            .build();
    this.externalCalls = externalCalls;
    this.circuitBreaker = circuitBreaker;
    this.baseUrl = baseUrl;
    this.failureThreshold = failureThreshold;
    this.openDuration = openDuration;
  }

  @Override
  public KofiaResponse collect(KofiaDataset dataset, LocalDate from, LocalDate to) {
    if (dataset.requiresSingleDateRequest() && !from.equals(to))
      throw new IllegalArgumentException(dataset.name() + " Dataset은 일자별 요청이 필요합니다.");
    Map<String, Object> search = dataset.requestParameters(from, to);
    Map<String, Object> request = Map.of("dmSearch", search);
    JsonNode response;
    response =
        circuitBreaker.execute(
            "KOFIA",
            failureThreshold,
            openDuration,
            () ->
                externalCalls.execute(
                    new ExternalApiCallExecutor.Call(
                        "kofia." + dataset.name(),
                        "KOFIA",
                        dataset.name(),
                        "POST",
                        baseUrl + dataset.path(),
                        request.toString()),
                    () ->
                        client
                            .post()
                            .uri(dataset.path())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(JsonNode.class)));
    if (response == null || !response.path("ds1").isArray())
      throw new IllegalStateException("KOFIA 응답에 ds1 배열이 없습니다.");
    List<KofiaRow> rows = new ArrayList<>();
    int rowNumber = 0;
    for (JsonNode row : response.path("ds1")) {
      rowNumber++;
      String date = row.path("TMPV1").asText();
      LocalDate baseDate = to;
      if (dataset.collectionMode() == KofiaDataset.CollectionMode.DATE_RANGE
          || dataset.collectionMode() == KofiaDataset.CollectionMode.MONTH_RANGE) {
        if (!date.matches("\\d{8}")) {
          if (isSummary(row)) continue;
          throw new IllegalStateException("KOFIA 행의 기준일(TMPV1)이 올바르지 않습니다: " + date);
        }
        baseDate = LocalDate.parse(date, DATE);
      }
      rows.add(
          new KofiaRow(
              baseDate,
              dataset.rowKey(row, rowNumber),
              rowType(row),
              dataset.entityCode(row),
              dataset.entityName(row),
              row));
    }
    return new KofiaResponse(response, Map.copyOf(search), List.copyOf(rows));
  }

  private boolean isSummary(JsonNode row) {
    return List.of("합계", "평균", "소계").stream()
        .anyMatch(
            value ->
                value.equals(row.path("TMPV1").asText())
                    || value.equals(row.path("TMPV2").asText()));
  }

  private String rowType(JsonNode row) {
    String values = row.path("TMPV1").asText() + "|" + row.path("TMPV2").asText();
    if (values.contains("합계")) return "TOTAL";
    if (values.contains("평균")) return "AVERAGE";
    if (values.contains("소계")) return "SUBTOTAL";
    return "DATA";
  }
}
