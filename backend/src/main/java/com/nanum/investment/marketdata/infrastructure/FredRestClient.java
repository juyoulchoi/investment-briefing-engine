package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.common.infrastructure.external.*;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class FredRestClient implements FredClient {
  private static final int PAGE_SIZE = 100000;
  private static final DateTimeFormatter FRED_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX");

  private final RestClient client;
  private final String baseUrl;
  private final String apiKey;
  private final ExternalApiRetryExecutor retry;
  private final CircuitBreakerSupport circuitBreaker;
  private final ExternalApiLogService logs;
  private final FredRequestRateLimiter limiter;
  private final int failureThreshold;
  private final Duration openDuration;

  public FredRestClient(
      @Value("${fred.base-url}") String baseUrl,
      @Value("${fred.api-key:}") String apiKey,
      @Value("${fred.connect-timeout:5s}") Duration connectTimeout,
      @Value("${fred.read-timeout:30s}") Duration readTimeout,
      @Value("${fred.circuit-breaker.failure-threshold:5}") int failureThreshold,
      @Value("${fred.circuit-breaker.open-duration:60s}") Duration openDuration,
      ExternalApiRetryExecutor retry,
      CircuitBreakerSupport circuitBreaker,
      ExternalApiLogService logs,
      FredRequestRateLimiter limiter) {
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
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.retry = retry;
    this.circuitBreaker = circuitBreaker;
    this.logs = logs;
    this.limiter = limiter;
    this.failureThreshold = failureThreshold;
    this.openDuration = openDuration;
  }

  @Override
  public SeriesMetadata metadata(String requestedCode) {
    String code = normalize(requestedCode);
    JsonNode body = get("/series", code, 0, null, null, "lin", "avg");
    JsonNode series = body.path("seriess").path(0);
    if (series.isMissingNode())
      throw new IllegalArgumentException("존재하지 않는 FRED Series입니다: " + code);
    return new SeriesMetadata(
        code,
        series.path("title").asText(code),
        date(series.path("observation_start").asText(null)),
        date(series.path("observation_end").asText(null)),
        series.path("frequency").asText(null),
        series.path("frequency_short").asText(null),
        series.path("units").asText(null),
        series.path("seasonal_adjustment").asText(null),
        time(series.path("last_updated").asText(null)));
  }

  @Override
  public List<Observation> observations(
      String requestedCode, LocalDate from, LocalDate to, String units, String aggregation) {
    String code = normalize(requestedCode);
    List<Observation> values = new ArrayList<>();
    for (int offset = 0; ; offset += PAGE_SIZE) {
      JsonNode body = get("/series/observations", code, offset, from, to, units, aggregation);
      JsonNode observations = body.path("observations");
      for (JsonNode item : observations) {
        String raw = item.path("value").asText(".");
        values.add(
            new Observation(
                LocalDate.parse(item.path("date").asText()),
                ".".equals(raw) ? null : new BigDecimal(raw),
                date(item.path("realtime_start").asText(null)),
                date(item.path("realtime_end").asText(null))));
      }
      int count = body.path("count").asInt(observations.size());
      if (offset + observations.size() >= count || observations.size() < PAGE_SIZE) break;
    }
    return values;
  }

  private JsonNode get(
      String path,
      String code,
      int offset,
      LocalDate from,
      LocalDate to,
      String units,
      String aggregation) {
    requireApiKey();
    limiter.acquire(0);
    OffsetDateTime requestedAt = OffsetDateTime.now();
    String url = baseUrl + path + "?series_id=" + code + "&api_key=" + apiKey;
    try {
      JsonNode body =
          circuitBreaker.execute(
              "FRED",
              failureThreshold,
              openDuration,
              () ->
                  retry.execute(
                      "fred." + code,
                      () ->
                          client
                              .get()
                              .uri(
                                  builder -> {
                                    builder
                                        .path(path)
                                        .queryParam("series_id", code)
                                        .queryParam("api_key", apiKey)
                                        .queryParam("file_type", "json");
                                    if (from != null) {
                                      builder
                                          .queryParam("observation_start", from)
                                          .queryParam("observation_end", to)
                                          .queryParam("sort_order", "asc")
                                          .queryParam("units", validUnits(units))
                                          .queryParam(
                                              "aggregation_method", validAggregation(aggregation))
                                          .queryParam("limit", PAGE_SIZE)
                                          .queryParam("offset", offset);
                                    }
                                    return builder.build();
                                  })
                              .retrieve()
                              .body(JsonNode.class)));
      if (body == null || body.has("error_code"))
        throw new IllegalStateException("FRED 응답을 처리하지 못했습니다: " + code);
      logs.save(
          UUID.randomUUID().toString(),
          "FRED",
          code,
          "GET",
          url,
          null,
          200,
          body.toString(),
          true,
          0,
          requestedAt,
          null);
      return body;
    } catch (RuntimeException error) {
      Integer status =
          error instanceof RestClientResponseException response
              ? response.getStatusCode().value()
              : null;
      logs.save(
          UUID.randomUUID().toString(),
          "FRED",
          code,
          "GET",
          url,
          null,
          status,
          null,
          false,
          0,
          requestedAt,
          error.getMessage());
      throw error;
    }
  }

  private void requireApiKey() {
    if (apiKey == null || apiKey.isBlank())
      throw new IllegalStateException("FRED_API_KEY가 설정되지 않았습니다.");
  }

  private String normalize(String code) {
    if (code == null || code.isBlank())
      throw new IllegalArgumentException("FRED Series 코드가 필요합니다.");
    return code.trim().toUpperCase(Locale.ROOT);
  }

  private String validUnits(String value) {
    return Set.of("lin", "chg", "ch1", "pch", "pc1", "pca", "cch", "cca", "log").contains(value)
        ? value
        : "lin";
  }

  private String validAggregation(String value) {
    return Set.of("avg", "sum", "eop").contains(value) ? value : "avg";
  }

  private LocalDate date(String value) {
    return value == null || value.isBlank() ? null : LocalDate.parse(value);
  }

  private OffsetDateTime time(String value) {
    return value == null || value.isBlank() ? null : OffsetDateTime.parse(value, FRED_TIME);
  }
}
