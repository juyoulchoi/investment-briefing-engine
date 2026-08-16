package com.nanum.investment.marketdata;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/krx")
public class KrxMarketDataController {
    private final KrxMarketDataService krx;

    public KrxMarketDataController(KrxMarketDataService krx) {
        this.krx = krx;
    }

    @GetMapping("/api-methods")
    public List<String> apiMethods() {
        return Arrays.stream(KrxDataset.values()).map(KrxDataset::apiMethod).toList();
    }

    @PostMapping("/{apiMethod}")
    public KrxMarketDataService.CollectionResult collect(
            @PathVariable String apiMethod,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        return krx.collect(KrxDataset.fromApiMethod(apiMethod), baseDate);
    }

    @GetMapping("/{apiMethod}")
    public List<Map<String, Object>> find(
            @PathVariable String apiMethod,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam(defaultValue = "100") int limit) {
        return krx.find(KrxDataset.fromApiMethod(apiMethod), baseDate, limit);
    }
}
