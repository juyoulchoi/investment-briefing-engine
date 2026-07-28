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
        this.jdbc = jdbc; this.json = json; this.authKey = authKey;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Transactional
    public CollectionResult collect(KrxDataset dataset, LocalDate date) {
        if (!StringUtils.hasText(authKey)) throw new IllegalStateException("KRX_AUTH_KEY가 필요합니다.");
        JsonNode response = client.get().uri(uri -> uri.path(dataset.path())
                .queryParam("basDd", date.format(DateTimeFormatter.BASIC_ISO_DATE)).build())
                .header("AUTH_KEY", authKey).retrieve().body(JsonNode.class);
        JsonNode rows = response == null ? null : response.path("OutBlock_1");
        if (rows == null || !rows.isArray()) throw new IllegalStateException("KRX 응답에 OutBlock_1 배열이 없습니다.");
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
            if (StringUtils.hasText(value)) return "IDX_CLSS".equals(key) ? value + "|" + row.path("IDX_NM").asText().trim() : value;
        }
        throw new IllegalStateException(dataset + " 행의 고유키가 비어 있습니다: " + json.valueToTree(row));
    }
    public record CollectionResult(String dataset, LocalDate baseDate, int receivedCount, long storedCount) {}
}
