package com.nanum.investment.marketdata;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {
    private final KrxMarketDataService krx;
    private final OverseasStockService overseas;
    private final YahooIndexService yahooIndices;
    private final FredBondYieldService fredBonds;

    public MarketDataController(KrxMarketDataService krx, OverseasStockService overseas, YahooIndexService yahooIndices, FredBondYieldService fredBonds) {
        this.krx = krx;
        this.overseas = overseas;
        this.yahooIndices = yahooIndices;
        this.fredBonds = fredBonds;
    }

    @GetMapping("/indices")
    public List<Map<String,Object>> indices(){return yahooIndices.indices();}

    @PostMapping("/indices/collect")
    public List<YahooIndexService.CollectionResult> collectIndices(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return yahooIndices.collectAll(from,to);}

    @PostMapping("/indices/{indexCode}/history/collect")
    public YahooIndexService.CollectionResult collectIndexHistory(@PathVariable String indexCode,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return yahooIndices.collect(indexCode,from,to);}

    @PostMapping("/indices/{indexCode}/refresh")
    public YahooIndexService.CollectionResult refreshIndex(@PathVariable String indexCode){return yahooIndices.refresh(indexCode);}

    @GetMapping("/indices/{indexCode}/history")
    public List<Map<String,Object>> indexHistory(@PathVariable String indexCode,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return yahooIndices.history(indexCode,from,to);}

    @GetMapping("/indices/{indexCode}")
    public Map<String,Object> latestIndex(@PathVariable String indexCode){return yahooIndices.latest(indexCode);}
    @PostMapping("/bonds/collect")
    public FredBondYieldService.CollectionResult collectBonds(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return fredBonds.collect(from,to);}

    @PostMapping("/bonds/refresh")
    public FredBondYieldService.RefreshResult refreshBonds(){return fredBonds.refreshLatest();}

    @GetMapping("/bonds")
    public List<Map<String,Object>> bonds(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return fredBonds.history(from,to);}

    @GetMapping("/overseas")
    public List<Map<String, Object>> overseasStocks() {
        return overseas.findAll();
    }
    @GetMapping("/domestic")
    public List<Map<String, Object>> domesticStocks() {
        return krx.findLatestStocks();
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
