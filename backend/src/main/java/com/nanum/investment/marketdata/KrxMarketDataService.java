package com.nanum.investment.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class KrxMarketDataService {
    private final JdbcClient jdbc;
    private final ObjectMapper json;
    private final RestClient client;
    private final String authKey;

    public KrxMarketDataService(JdbcClient jdbc, ObjectMapper json,
            @Value("${krx.base-url}") String baseUrl, @Value("${krx.auth-key:}") String authKey) {
        this.jdbc = jdbc;
        this.json = json;
        this.authKey = authKey;
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
        for (JsonNode row : rows) {
            LocalDate rowDate = parseDate(row.path("BAS_DD").asText(), date);
            jdbc.sql("""
                    INSERT INTO tb_krx_dataset_row(dataset_code, base_date, row_key, payload)
                    VALUES (:dataset, :date, :key, CAST(:payload AS jsonb))
                    ON CONFLICT (dataset_code, base_date, row_key)
                    DO UPDATE SET payload=EXCLUDED.payload, updated_at=CURRENT_TIMESTAMP
                    """).param("dataset", dataset.name()).param("date", rowDate)
                    .param("key", rowKey(dataset, row)).param("payload", row.toString()).update();
            received++;
        }
        return new CollectionResult(dataset.name(), date, received, count(dataset, date));
    }

    public List<Map<String, Object>> find(KrxDataset dataset, LocalDate date, int limit) {
        return jdbc.sql("""
                SELECT dataset_code, base_date, row_key, payload, updated_at
                FROM tb_krx_dataset_row WHERE dataset_code=:dataset AND base_date=:date
                ORDER BY row_key LIMIT :limit
                """).param("dataset", dataset.name()).param("date", date).param("limit", Math.min(limit, 1000))
                .query().listOfRows();
    }

    public List<Map<String, Object>> findLatestStocks() {
        return jdbc.sql("""
                WITH latest AS (
                  SELECT max(base_date) AS base_date FROM tb_krx_dataset_row
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
                  FROM tb_krx_dataset_row r, latest
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
        return jdbc.sql("SELECT count(*) FROM tb_krx_dataset_row WHERE dataset_code=:dataset AND base_date=:date")
                .param("dataset", dataset.name()).param("date", date).query(Long.class).single();
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











