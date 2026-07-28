package com.nanum.investment.marketdata;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {
    private final KrxMarketDataService krx;
    private final OverseasStockService overseas;

    public MarketDataController(KrxMarketDataService krx, OverseasStockService overseas) {
        this.krx = krx;
        this.overseas = overseas;
    }

    @GetMapping("/krx/datasets")
    public List<String> datasets() {
        return Arrays.stream(KrxDataset.values()).map(Enum::name).toList();
    }

    @PostMapping("/krx/{dataset}/collect")
    public KrxMarketDataService.CollectionResult collect(@PathVariable KrxDataset dataset,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        return krx.collect(dataset, baseDate);
    }

    @GetMapping("/krx/{dataset}")
    public List<Map<String, Object>> krxRows(@PathVariable KrxDataset dataset,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam(defaultValue = "100") int limit) {
        return krx.find(dataset, baseDate, limit);
    }

    @PostMapping("/overseas/{symbol}/history/collect")
    public OverseasStockService.HistoryCollectionResult collectHistory(@PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return overseas.collectHistory(symbol, from, to);
    }

    @GetMapping("/overseas/{symbol}/history")
    public List<Map<String, Object>> overseasHistory(@PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return overseas.findHistory(symbol, from, to);
    }

    @PostMapping("/overseas/{symbol}/refresh")
    public Map<String, Object> refresh(@PathVariable String symbol) {
        return overseas.refresh(symbol);
    }

    @GetMapping("/overseas/{symbol}")
    public Map<String, Object> overseas(@PathVariable String symbol) {
        return overseas.find(symbol);
    }
}
