package com.nanum.investment.marketdata.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.common.infrastructure.external.CollectionResult;
import com.nanum.investment.common.infrastructure.external.CircuitBreakerSupport;
import com.nanum.investment.common.infrastructure.external.ExternalApiRetryExecutor;
import com.nanum.investment.common.infrastructure.external.ExternalRestClientFactory;
import com.nanum.investment.holding.application.HoldingPriceSyncService;
import com.nanum.investment.marketdata.domain.KrxDataset;
import com.nanum.investment.marketdata.infrastructure.KrxIndexDailyCollector;
import com.nanum.investment.marketdata.infrastructure.KrxRequestRateLimiter;
import com.nanum.investment.marketdata.infrastructure.KrxBondTradingDailyCollector;
import com.nanum.investment.marketdata.infrastructure.KrxDerivativeDailyCollector;
import java.time.LocalDate;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class KrxMarketDataService {
  private static final Set<KrxDataset> STOCK_DAILY_DATASETS =
      Set.of(KrxDataset.KOSPI_STOCK_DAILY, KrxDataset.KOSDAQ_STOCK_DAILY,
          KrxDataset.KONEX_STOCK_DAILY, KrxDataset.SUBSCRIPTION_WARRANT_DAILY,
          KrxDataset.SUBSCRIPTION_RIGHT_DAILY, KrxDataset.ETF_DAILY,
          KrxDataset.ETN_DAILY, KrxDataset.ELW_DAILY);
  private static final Set<KrxDataset> STOCK_MASTER_DATASETS =
      Set.of(KrxDataset.KOSPI_STOCK_MASTER, KrxDataset.KOSDAQ_STOCK_MASTER,
          KrxDataset.ALL_STOCK_MASTER, KrxDataset.KONEX_STOCK_MASTER);
  private static final Set<KrxDataset> INDEX_DAILY_DATASETS =
      Set.of(KrxDataset.KRX_INDEX_DAILY, KrxDataset.KOSPI_INDEX_DAILY,
          KrxDataset.KOSDAQ_INDEX_DAILY, KrxDataset.BOND_INDEX_DAILY,
          KrxDataset.DERIVATIVE_INDEX_DAILY);
  private static final Set<KrxDataset> DERIVATIVE_DAILY_DATASETS =
      Set.of(KrxDataset.FUTURES_DAILY, KrxDataset.KOSPI_STOCK_FUTURES_DAILY,
          KrxDataset.KOSDAQ_STOCK_FUTURES_DAILY, KrxDataset.OPTIONS_DAILY,
          KrxDataset.KOSPI_STOCK_OPTIONS_DAILY, KrxDataset.KOSDAQ_STOCK_OPTIONS_DAILY);
  private static final Set<KrxDataset> BOND_TRADING_DAILY_DATASETS =
      Set.of(KrxDataset.GOVERNMENT_BOND_DAILY, KrxDataset.GENERAL_BOND_DAILY,
          KrxDataset.SMALL_BOND_DAILY);

  private final JdbcClient jdbc;
  private final ObjectMapper json;
  private final RestClient client;
  private final String authKey;
  private final HoldingPriceSyncService holdingPriceSync;
  private final ExternalApiRetryExecutor retry;
  private final KrxIndexDailyCollector indexDailyCollector;
  private final KrxDerivativeDailyCollector derivativeDailyCollector;
  private final KrxBondTradingDailyCollector bondTradingDailyCollector;
  private final KrxRequestRateLimiter rateLimiter;
  private final CircuitBreakerSupport circuitBreaker;
  private final int circuitFailureThreshold;
  private final Duration circuitOpenDuration;

  public KrxMarketDataService(
      JdbcClient jdbc,
      ObjectMapper json,
      @Value("${krx.base-url}") String baseUrl,
      @Value("${krx.auth-key:}") String authKey,
      @Value("${krx.connect-timeout:5s}") Duration connectTimeout,
      @Value("${krx.read-timeout:30s}") Duration readTimeout,
      @Value("${krx.circuit-breaker.failure-threshold:5}") int circuitFailureThreshold,
      @Value("${krx.circuit-breaker.open-duration:60s}") Duration circuitOpenDuration,
      HoldingPriceSyncService holdingPriceSync,
      ExternalRestClientFactory clients,
      ExternalApiRetryExecutor retry,
      CircuitBreakerSupport circuitBreaker,
      KrxRequestRateLimiter rateLimiter,
      KrxIndexDailyCollector indexDailyCollector,
      KrxDerivativeDailyCollector derivativeDailyCollector,
      KrxBondTradingDailyCollector bondTradingDailyCollector) {
    this.jdbc = jdbc;
    this.json = json;
    this.authKey = authKey;
    this.holdingPriceSync = holdingPriceSync;
    this.client = clients.builder(baseUrl, connectTimeout, readTimeout).build();
    this.retry = retry;
    this.rateLimiter = rateLimiter;
    this.circuitBreaker = circuitBreaker;
    this.circuitFailureThreshold = circuitFailureThreshold;
    this.circuitOpenDuration = circuitOpenDuration;
    this.indexDailyCollector = indexDailyCollector;
    this.derivativeDailyCollector = derivativeDailyCollector;
    this.bondTradingDailyCollector = bondTradingDailyCollector;
  }

  @Transactional
  public CollectionResult collect(KrxDataset dataset, LocalDate date) {
    if (!StringUtils.hasText(authKey)) throw new IllegalStateException("KRX_AUTH_KEY가 필요합니다.");
    rateLimiter.acquire();
    JsonNode response = circuitBreaker.execute(
        "KRX:" + dataset.name(), circuitFailureThreshold, circuitOpenDuration,
        () -> retry.execute("krx." + dataset.name(), () -> client
                    .get()
                    .uri(
                        uri ->
                            uri.path(dataset.path())
                                .queryParam("basDd", date.format(DateTimeFormatter.BASIC_ISO_DATE))
                                .build())
                    .header("AUTH_KEY", authKey)
                    .retrieve()
                    .body(JsonNode.class)));
    JsonNode rows = response == null ? null : response.path("OutBlock_1");
    if (rows == null || !rows.isArray())
      throw new IllegalStateException("KRX 응답에 OutBlock_1 배열이 없습니다.");
    int received = 0;
    Set<LocalDate> collectedDates = new HashSet<>();
    for (JsonNode row : rows) {
      LocalDate rowDate = parseDate(row.path("BAS_DD").asText(), date);
      collectedDates.add(rowDate);
      jdbc.sql(
              """
                    INSERT INTO tb_krx_data_row(dataset_code, base_date, row_key, payload)
                    VALUES (:dataset, :date, :key, CAST(:payload AS jsonb))
                    ON CONFLICT (dataset_code, base_date, row_key)
                    DO UPDATE SET payload=EXCLUDED.payload, updated_at=CURRENT_TIMESTAMP
                    """)
          .param("dataset", dataset.name())
          .param("date", rowDate)
          .param("key", rowKey(dataset, row))
          .param("payload", row.toString())
          .update();
      received++;
    }
    if (STOCK_DAILY_DATASETS.contains(dataset)) {
      collectedDates.forEach(collectedDate -> syncStockPrices(dataset, collectedDate));
      holdingPriceSync.refreshMarket("KO");
    }
    if (STOCK_MASTER_DATASETS.contains(dataset)) syncStockMaster(dataset, date);
    if (INDEX_DAILY_DATASETS.contains(dataset)) indexDailyCollector.normalize(dataset, date);
    if (DERIVATIVE_DAILY_DATASETS.contains(dataset)) derivativeDailyCollector.normalize(dataset, date);
    if (BOND_TRADING_DAILY_DATASETS.contains(dataset)) bondTradingDailyCollector.normalize(dataset, date);
    return new CollectionResult(dataset.name(), date, received, count(dataset, date));
  }

  public List<Map<String, Object>> find(KrxDataset dataset, LocalDate date, int limit) {
    return jdbc.sql(
            """
                SELECT dataset_code, base_date, row_key, payload, updated_at
                FROM tb_krx_data_row WHERE dataset_code=:dataset AND base_date=:date
                ORDER BY row_key LIMIT :limit
                """)
        .param("dataset", dataset.name())
        .param("date", date)
        .param("limit", Math.min(limit, 1000))
        .query()
        .listOfRows();
  }

  public List<Map<String, Object>> findLatestStocks() {
    return jdbc.sql(
            """
                WITH latest AS (
                  SELECT max(base_date) AS base_date FROM tb_krx_data_row
                  WHERE dataset_code IN ('KOSPI_STOCK_DAILY', 'KOSDAQ_STOCK_DAILY', 'ETF_DAILY')
                ), prices AS (
                  SELECT DISTINCT ON (payload->>'ISU_CD') payload->>'ISU_CD' AS stock_code,
                    CASE WHEN dataset_code = 'ETF_DAILY' THEN 'ETF' ELSE payload->>'MKT_NM' END AS exchange_name,
                    r.base_date AS trading_day,
                    NULLIF(replace(payload->>'TDD_CLSPRC', ',', ''), '')::numeric AS price,
                    NULLIF(replace(payload->>'TDD_CLSPRC', ',', ''), '')::numeric
                      - COALESCE(NULLIF(replace(payload->>'CMPPREVDD_PRC', ',', ''), '')::numeric, 0) AS previous_close,
                    NULLIF(replace(payload->>'CMPPREVDD_PRC', ',', ''), '')::numeric AS change_amount,
                    CASE WHEN NULLIF(payload->>'FLUC_RT', '') IS NULL THEN NULL ELSE (payload->>'FLUC_RT') || '%' END AS change_percent,
                    NULLIF(replace(payload->>'ACC_TRDVOL', ',', ''), '')::bigint AS volume, r.updated_at
                  FROM tb_krx_data_row r, latest
                  WHERE dataset_code IN ('KOSPI_STOCK_DAILY', 'KOSDAQ_STOCK_DAILY', 'ETF_DAILY')
                    AND r.base_date = latest.base_date
                  ORDER BY payload->>'ISU_CD', CASE WHEN dataset_code = 'ETF_DAILY' THEN 0 ELSE 1 END
                )
                SELECT s.stock_code AS symbol, s.stock_name AS company_name,
                  COALESCE(p.exchange_name, s.asset_type) AS exchange_name,
                  'KRW' AS currency, p.trading_day, p.price, p.previous_close,
                  p.change_amount, p.change_percent, p.volume, 'KRX' AS provider,
                  COALESCE(p.updated_at, s.updated_at) AS updated_at,
                  s.market_scope AS account_type, s.market_scope, ss.stk_grade AS stock_grade
                FROM tb_hold s
                LEFT JOIN tb_stk_set ss
                  ON ss.acct_tp = s.market_scope AND ss.stk_cd = s.stock_code
                LEFT JOIN prices p ON p.stock_code = s.stock_code
                WHERE s.active_yn = 'Y' AND s.listing_scope = 'DOMESTIC'
                ORDER BY s.market_scope, s.asset_type, s.stock_name
                """)
        .query()
        .listOfRows();
  }

  private long count(KrxDataset dataset, LocalDate date) {
    return jdbc.sql(
            "SELECT count(*) FROM tb_krx_data_row WHERE dataset_code=:dataset AND base_date=:date")
        .param("dataset", dataset.name())
        .param("date", date)
        .query(Long.class)
        .single();
  }

  private int syncStockPrices(KrxDataset dataset, LocalDate date) {
    return jdbc.sql(
            """
                INSERT INTO "TB_PRC_DAY"
                    ("STK_ID", "MKT_CD", "STK_CD", "TRADE_DT", "OPEN_PRC", "HIGH_PRC", "LOW_PRC",
                     "CLS_PRC", "ADJ_CLS_PRC", "VOL", "PRVDR", "DATA_SRC_CD")
                SELECT DISTINCT ON (r."BASE_DT", r."PAYLOAD"->>'ISU_CD')
                    s."STK_ID", 'KO', r."PAYLOAD"->>'ISU_CD', r."BASE_DT",
                    NULLIF(replace(r."PAYLOAD"->>'TDD_OPNPRC', ',', ''), '')::numeric,
                    NULLIF(replace(r."PAYLOAD"->>'TDD_HGPRC', ',', ''), '')::numeric,
                    NULLIF(replace(r."PAYLOAD"->>'TDD_LWPRC', ',', ''), '')::numeric,
                    NULLIF(replace(r."PAYLOAD"->>'TDD_CLSPRC', ',', ''), '')::numeric,
                    NULLIF(replace(r."PAYLOAD"->>'TDD_CLSPRC', ',', ''), '')::numeric,
                    COALESCE(NULLIF(replace(r."PAYLOAD"->>'ACC_TRDVOL', ',', ''), '')::bigint, 0),
                    'KRX', 'KRX'
                FROM "TB_KRX_DATA_ROW" r
                JOIN "TB_STK" s
                  ON s."MKT_CD" = 'KO'
                 AND s."STK_CD" = r."PAYLOAD"->>'ISU_CD'
                WHERE r."DATA_CD" = :dataset
                  AND r."BASE_DT" = :date
                  AND NULLIF(r."PAYLOAD"->>'TDD_CLSPRC', '') IS NOT NULL
                ORDER BY r."BASE_DT", r."PAYLOAD"->>'ISU_CD'
                ON CONFLICT ("STK_CD", "TRADE_DT") DO UPDATE
                SET "OPEN_PRC" = EXCLUDED."OPEN_PRC",
                    "HIGH_PRC" = EXCLUDED."HIGH_PRC",
                    "LOW_PRC" = EXCLUDED."LOW_PRC",
                    "CLS_PRC" = EXCLUDED."CLS_PRC",
                    "ADJ_CLS_PRC" = EXCLUDED."ADJ_CLS_PRC",
                    "VOL" = EXCLUDED."VOL",
                    "PRVDR" = EXCLUDED."PRVDR",
                    "DATA_SRC_CD" = EXCLUDED."DATA_SRC_CD",
                    "DATA_STS" = 'FRESH',
                    "COLLECT_DTTM" = CURRENT_TIMESTAMP,
                    "MOD_DT" = CURRENT_TIMESTAMP
                """)
        .param("dataset", dataset.name())
        .param("date", date)
        .update();
  }

  private int syncStockMaster(KrxDataset dataset, LocalDate date) {
    return jdbc.sql("""
        INSERT INTO "TB_STK" ("MKT_CD","STK_CD","STK_NM","LIST_SCOPE","ASSET_TP","EXCH_NM",
          "CURR","PRVDR","ACTV_YN","TICKER","CNTRY_CD","CURR_CD","AST_TP","STK_GRADE",
          "REG_BUY_YN","ADD_BUY_YN","REBUY_YN","USE_YN","DEL_YN","CRT_USR_ID","UPD_USR_ID")
        SELECT 'KO', COALESCE(NULLIF("PAYLOAD"->>'ISU_CD',''), "PAYLOAD"->>'ISU_SRT_CD'),
          COALESCE(NULLIF("PAYLOAD"->>'ISU_NM',''), "PAYLOAD"->>'ISU_ABBRV'), 'DOMESTIC',
          CASE WHEN COALESCE("PAYLOAD"->>'SECUGRP_NM','') ILIKE '%ETF%' THEN 'ETF' ELSE 'STOCK' END,
          COALESCE(NULLIF("PAYLOAD"->>'MKT_NM',''),'KRX'), 'KRW', 'KRX', 'Y',
          COALESCE(NULLIF("PAYLOAD"->>'ISU_SRT_CD',''), "PAYLOAD"->>'ISU_CD'), 'KR', 'KRW',
          CASE WHEN COALESCE("PAYLOAD"->>'SECUGRP_NM','') ILIKE '%ETF%' THEN 'ETF' ELSE 'STOCK' END,
          'CORE','N','N','N','Y','N','SYSTEM','SYSTEM'
        FROM "TB_KRX_DATA_ROW" WHERE "DATA_CD"=:dataset AND "BASE_DT"=:date
          AND COALESCE(NULLIF("PAYLOAD"->>'ISU_CD',''), "PAYLOAD"->>'ISU_SRT_CD') IS NOT NULL
        ON CONFLICT ("STK_CD") DO UPDATE SET "STK_NM"=EXCLUDED."STK_NM",
          "EXCH_NM"=EXCLUDED."EXCH_NM", "PRVDR"='KRX', "ACTV_YN"='Y', "USE_YN"='Y',
          "DEL_YN"='N', "MOD_DT"=CURRENT_TIMESTAMP, "UPD_DTTM"=CURRENT_TIMESTAMP
        """).param("dataset", dataset.name()).param("date", date).update();
  }

  private LocalDate parseDate(String value, LocalDate fallback) {
    return StringUtils.hasText(value)
        ? LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
        : fallback;
  }

  private String rowKey(KrxDataset dataset, JsonNode row) {
    for (String key : dataset.keys()) {
      String value = row.path(key).asText().trim();
      if (StringUtils.hasText(value))
        return "IDX_CLSS".equals(key) ? value + "|" + row.path("IDX_NM").asText().trim() : value;
    }
    throw new IllegalStateException(dataset + " 행의 고유키가 비어 있습니다: " + json.valueToTree(row));
  }

  public record CollectionResult(
      String dataset, LocalDate baseDate, int receivedCount, long storedCount) {}
}
