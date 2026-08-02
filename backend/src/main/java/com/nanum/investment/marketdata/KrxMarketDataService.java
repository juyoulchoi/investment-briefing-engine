package com.nanum.investment.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.service.HoldingPriceSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class KrxMarketDataService {
    private static final Set<KrxDataset> STOCK_DAILY_DATASETS = Set.of(
            KrxDataset.KOSPI_STOCK_DAILY,
            KrxDataset.KOSDAQ_STOCK_DAILY,
            KrxDataset.ETF_DAILY);

    private final JdbcClient jdbc;
    private final ObjectMapper json;
    private final RestClient client;
    private final String authKey;
    private final HoldingPriceSyncService holdingPriceSync;

    public KrxMarketDataService(JdbcClient jdbc, ObjectMapper json,
            @Value("${krx.base-url}") String baseUrl, @Value("${krx.auth-key:}") String authKey,
            HoldingPriceSyncService holdingPriceSync) {
        this.jdbc = jdbc;
        this.json = json;
        this.authKey = authKey;
        this.holdingPriceSync = holdingPriceSync;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Transactional
    public CollectionResult collect(KrxDataset dataset, LocalDate date) {
        if (!StringUtils.hasText(authKey))
            throw new IllegalStateException("KRX_AUTH_KEY가 필요합니다.");
        JsonNode response = client.get().uri(uri -> uri.path(dataset.path())
                .queryParam("basDd", date.format(DateTimeFormatter.BASIC_ISO_DATE)).build())
                .header("AUTH_KEY", authKey).retrieve().body(JsonNode.class);
        JsonNode rows = response == null ? null : response.path("OutBlock_1");
        if (rows == null || !rows.isArray())
            throw new IllegalStateException("KRX 응답에 OutBlock_1 배열이 없습니다.");
        int received = 0;
        Set<LocalDate> collectedDates = new HashSet<>();
        for (JsonNode row : rows) {
            LocalDate rowDate = parseDate(row.path("BAS_DD").asText(), date);
            collectedDates.add(rowDate);
            jdbc.sql("""
                    INSERT INTO tb_krx_data_row(dataset_code, base_date, row_key, payload)
                    VALUES (:dataset, :date, :key, CAST(:payload AS jsonb))
                    ON CONFLICT (dataset_code, base_date, row_key)
                    DO UPDATE SET payload=EXCLUDED.payload, updated_at=CURRENT_TIMESTAMP
                    """).param("dataset", dataset.name()).param("date", rowDate)
                    .param("key", rowKey(dataset, row)).param("payload", row.toString()).update();
            received++;
        }
        if (STOCK_DAILY_DATASETS.contains(dataset)) {
            collectedDates.forEach(collectedDate -> syncStockPrices(dataset, collectedDate));
            holdingPriceSync.refreshMarket("KO");
        }
        return new CollectionResult(dataset.name(), date, received, count(dataset, date));
    }

    public List<Map<String, Object>> find(KrxDataset dataset, LocalDate date, int limit) {
        return jdbc.sql("""
                SELECT dataset_code, base_date, row_key, payload, updated_at
                FROM tb_krx_data_row WHERE dataset_code=:dataset AND base_date=:date
                ORDER BY row_key LIMIT :limit
                """).param("dataset", dataset.name()).param("date", date).param("limit", Math.min(limit, 1000))
                .query().listOfRows();
    }

    public List<Map<String, Object>> findLatestStocks() {
        return jdbc.sql("""
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
                """).query().listOfRows();
    }

    private long count(KrxDataset dataset, LocalDate date) {
        return jdbc.sql("SELECT count(*) FROM tb_krx_data_row WHERE dataset_code=:dataset AND base_date=:date")
                .param("dataset", dataset.name()).param("date", date).query(Long.class).single();
    }

    private int syncStockPrices(KrxDataset dataset, LocalDate date) {
        return jdbc.sql("""
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
                ON CONFLICT ("MKT_CD", "STK_CD", "TRADE_DT") DO UPDATE
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
                """).param("dataset", dataset.name()).param("date", date).update();
    }
    private LocalDate parseDate(String value, LocalDate fallback) {
        return StringUtils.hasText(value) ? LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE) : fallback;
    }

    private String rowKey(KrxDataset dataset, JsonNode row) {
        for (String key : dataset.keys()) {
            String value = row.path(key).asText().trim();
            if (StringUtils.hasText(value))
                return "IDX_CLSS".equals(key) ? value + "|" + row.path("IDX_NM").asText().trim() : value;
        }
        throw new IllegalStateException(dataset + " 행의 고유키가 비어 있습니다: " + json.valueToTree(row));
    }

    public record CollectionResult(String dataset, LocalDate baseDate, int receivedCount, long storedCount) {
    }
}











