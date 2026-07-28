package com.nanum.investment.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@Service
public class OverseasStockService {
    private final JdbcClient jdbc;
    private final RestClient client;

    public OverseasStockService(JdbcClient jdbc, @Value("${overseas.yahoo.base-url}") String baseUrl) {
        this.jdbc = jdbc;
        this.client = RestClient.builder().baseUrl(baseUrl)
                .defaultHeader("User-Agent", "Mozilla/5.0 investment-briefing-engine/1.0")
                .defaultHeader("Accept", "application/json").build();
    }

    public Map<String, Object> refresh(String requestedSymbol) {
        String symbol = normalize(requestedSymbol);
        JsonNode response = client.get().uri(uri -> uri.pathSegment(symbol)
                .queryParam("range", "5d").queryParam("interval", "1d")
                .queryParam("events", "div,splits").build()).retrieve().body(JsonNode.class);
        JsonNode chart = response == null ? null : response.path("chart");
        if (chart == null || !chart.path("error").isNull() || chart.path("result").isEmpty()) {
            throw new IllegalStateException("Yahoo Finance 시세를 받지 못했습니다: " + (chart == null ? "빈 응답" : chart.path("error")));
        }
        JsonNode result = chart.path("result").get(0);
        JsonNode meta = result.path("meta");
        JsonNode timestamps = result.path("timestamp");
        JsonNode quote = result.path("indicators").path("quote").get(0);
        int index = lastValidIndex(timestamps, quote.path("close"));
        BigDecimal price = decimal(quote.path("close").get(index));
        BigDecimal previous = index > 0 ? decimal(quote.path("close").get(index - 1)) : decimal(meta.path("chartPreviousClose"));
        BigDecimal change = price.subtract(previous);
        String percent = previous.signum() == 0 ? "0%" : change.multiply(BigDecimal.valueOf(100))
                .divide(previous, 4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
        String timezone = meta.path("exchangeTimezoneName").asText("UTC");
        LocalDate tradingDay = Instant.ofEpochSecond(timestamps.get(index).asLong()).atZone(ZoneId.of(timezone)).toLocalDate();

        jdbc.sql("""
            INSERT INTO tb_overseas_stock_quote(symbol,company_name,exchange_name,currency,trading_day,
              open_price,high_price,low_price,price,previous_close,change_amount,change_percent,volume,provider,payload)
            VALUES (:symbol,:name,:exchange,:currency,:day,:open,:high,:low,:price,:previous,:change,:percent,
              :volume,'YAHOO_FINANCE',CAST(:payload AS jsonb))
            ON CONFLICT(symbol) DO UPDATE SET company_name=EXCLUDED.company_name,exchange_name=EXCLUDED.exchange_name,
              currency=EXCLUDED.currency,trading_day=EXCLUDED.trading_day,open_price=EXCLUDED.open_price,
              high_price=EXCLUDED.high_price,low_price=EXCLUDED.low_price,price=EXCLUDED.price,
              previous_close=EXCLUDED.previous_close,change_amount=EXCLUDED.change_amount,
              change_percent=EXCLUDED.change_percent,volume=EXCLUDED.volume,provider=EXCLUDED.provider,
              payload=EXCLUDED.payload,updated_at=CURRENT_TIMESTAMP
            """).param("symbol", symbol).param("name", meta.path("longName").asText(meta.path("shortName").asText(symbol)))
            .param("exchange", meta.path("fullExchangeName").asText(meta.path("exchangeName").asText()))
            .param("currency", meta.path("currency").asText()).param("day", tradingDay)
            .param("open", decimal(quote.path("open").get(index))).param("high", decimal(quote.path("high").get(index)))
            .param("low", decimal(quote.path("low").get(index))).param("price", price).param("previous", previous)
            .param("change", change).param("percent", percent).param("volume", quote.path("volume").get(index).asLong())
            .param("payload", result.toString()).update();
        return find(symbol);
    }

    public Map<String, Object> find(String requestedSymbol) {
        var rows = jdbc.sql("""
            SELECT symbol,company_name,exchange_name,currency,trading_day,open_price,high_price,low_price,
              price,previous_close,change_amount,change_percent,volume,provider,updated_at
            FROM tb_overseas_stock_quote WHERE symbol=:symbol
            """).param("symbol", normalize(requestedSymbol)).query().listOfRows();
        if (rows.isEmpty()) throw new IllegalArgumentException("저장된 해외주식 시세가 없습니다.");
        return rows.getFirst();
    }

    private int lastValidIndex(JsonNode timestamps, JsonNode closes) {
        for (int i = Math.min(timestamps.size(), closes.size()) - 1; i >= 0; i--) {
            if (!closes.get(i).isNull() && closes.get(i).isNumber()) return i;
        }
        throw new IllegalStateException("Yahoo Finance 응답에 유효한 종가가 없습니다.");
    }
    private String normalize(String symbol) {
        String value = symbol == null ? "" : symbol.trim().toUpperCase();
        if (!value.matches("[A-Z0-9.^=:-]{1,30}")) throw new IllegalArgumentException("유효하지 않은 종목 심볼입니다.");
        return value;
    }
    private BigDecimal decimal(JsonNode value) {
        return value == null || value.isNull() ? BigDecimal.ZERO : value.decimalValue();
    }
}
