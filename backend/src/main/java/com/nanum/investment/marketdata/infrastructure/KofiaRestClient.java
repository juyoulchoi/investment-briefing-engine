package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.common.infrastructure.external.CircuitBreakerSupport;
import com.nanum.investment.common.infrastructure.external.ExternalApiLogService;
import com.nanum.investment.common.infrastructure.external.ExternalApiRetryExecutor;
import com.nanum.investment.marketdata.domain.KofiaDataset;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class KofiaRestClient implements KofiaClient {
  private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
  private final RestClient client;
  private final ExternalApiRetryExecutor retry;
  private final CircuitBreakerSupport circuitBreaker;
  private final ExternalApiLogService logs;
  private final String baseUrl;
  private final int failureThreshold;
  private final Duration openDuration;

  public KofiaRestClient(
      @Value("${kofia.base-url}") String baseUrl,
      @Value("${kofia.connect-timeout:5s}") Duration connectTimeout,
      @Value("${kofia.read-timeout:30s}") Duration readTimeout,
      @Value("${kofia.circuit-breaker.failure-threshold:5}") int failureThreshold,
      @Value("${kofia.circuit-breaker.open-duration:60s}") Duration openDuration,
      ExternalApiRetryExecutor retry,
      CircuitBreakerSupport circuitBreaker,
      ExternalApiLogService logs) {
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
    this.retry = retry;
    this.circuitBreaker = circuitBreaker;
    this.logs = logs;
    this.baseUrl = baseUrl;
    this.failureThreshold = failureThreshold;
    this.openDuration = openDuration;
  }

  @Override
  public KofiaResponse collect(KofiaDataset dataset, LocalDate from, LocalDate to) {
    Map<String, Object> search =
        Map.of(
            "tmpV40",
            "1000000",
            "tmpV41",
            "1",
            "tmpV1",
            "D",
            "tmpV45",
            from.format(DATE),
            "tmpV46",
            to.format(DATE),
            "OBJ_NM",
            dataset.objectName());
    Map<String, Object> request = Map.of("dmSearch", search);
    OffsetDateTime requestedAt = OffsetDateTime.now();
    JsonNode response;
    try {
      response =
          circuitBreaker.execute(
              "KOFIA",
              failureThreshold,
              openDuration,
              () ->
                  retry.execute(
                      () ->
                          client
                              .post()
                              .uri(dataset.path())
                              .contentType(MediaType.APPLICATION_JSON)
                              .body(request)
                              .retrieve()
                              .body(JsonNode.class)));
      logs.save(
          UUID.randomUUID().toString(),
          "KOFIA",
          dataset.name(),
          "POST",
          baseUrl + dataset.path(),
          request.toString(),
          200,
          response == null ? null : response.toString(),
          true,
          0,
          requestedAt,
          null);
    } catch (RuntimeException error) {
      Integer status =
          error instanceof RestClientResponseException responseError
              ? responseError.getStatusCode().value()
              : null;
      logs.save(
          UUID.randomUUID().toString(),
          "KOFIA",
          dataset.name(),
          "POST",
          baseUrl + dataset.path(),
          request.toString(),
          status,
          null,
          false,
          0,
          requestedAt,
          error.getMessage());
      throw error;
    }
    if (response == null || !response.path("ds1").isArray())
      throw new IllegalStateException("KOFIA 응답에 ds1 배열이 없습니다.");
    List<KofiaRow> rows = new ArrayList<>();
    for (JsonNode row : response.path("ds1")) {
      String date = row.path("TMPV1").asText();
      if (!date.matches("\\d{8}"))
        throw new IllegalStateException("KOFIA 행의 기준일(TMPV1)이 올바르지 않습니다: " + date);
      rows.add(new KofiaRow(LocalDate.parse(date, DATE), row));
    }
    return new KofiaResponse(response, List.copyOf(rows));
  }
}
