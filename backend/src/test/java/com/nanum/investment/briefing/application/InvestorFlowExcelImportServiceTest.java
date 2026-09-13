package com.nanum.investment.briefing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class InvestorFlowExcelImportServiceTest {
  @Test
  void classifiesMarketAndStockFilenames() {
    Path root = Path.of("investor-flow").toAbsolutePath();

    InvestorFlowExcelImportService.Metadata market =
        InvestorFlowExcelImportService.metadata(
            root, root.resolve("투자자별 거래실적 거래대금 순매수_20240101.xlsx"));
    InvestorFlowExcelImportService.Metadata stock =
        InvestorFlowExcelImportService.metadata(
            root,
            root.resolve("삼성전자").resolve("삼성전자 투자자별 거래실적 거래량 매도_20240101.xlsx"));

    assertThat(market.scopeType()).isEqualTo(InvestorFlowExcelImportService.ScopeType.MARKET);
    assertThat(market.metricType()).isEqualTo(InvestorFlowExcelImportService.MetricType.AMOUNT);
    assertThat(market.tradeType()).isEqualTo(InvestorFlowExcelImportService.TradeType.NET_BUY);
    assertThat(stock.scopeType()).isEqualTo(InvestorFlowExcelImportService.ScopeType.STOCK);
    assertThat(stock.stockName()).isEqualTo("삼성전자");
    assertThat(stock.metricType()).isEqualTo(InvestorFlowExcelImportService.MetricType.VOLUME);
    assertThat(stock.tradeType()).isEqualTo(InvestorFlowExcelImportService.TradeType.SELL);
  }
}
