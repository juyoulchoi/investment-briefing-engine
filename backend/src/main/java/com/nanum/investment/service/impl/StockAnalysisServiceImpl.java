package com.nanum.investment.service.impl;

import com.nanum.investment.domain.*;
import com.nanum.investment.request.RiskInput;
import com.nanum.investment.request.StockAnalysisInput;
import com.nanum.investment.response.BasicStockMetrics;
import com.nanum.investment.response.RiskResult;
import com.nanum.investment.response.StockAnalysisResult;
import com.nanum.investment.response.WeightResult;
import com.nanum.investment.service.StockAnalysisService;
import com.nanum.investment.service.MarketCalendarService;
import com.nanum.investment.service.calculator.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StockAnalysisServiceImpl implements StockAnalysisService {
    private final BasicStockMetricsCalculator basicStockMetricsCalculator;
    private final WeightCalculator weightCalculator;
    private final MarketPhaseCalculator marketPhaseCalculator;
    private final MarketDrawdownCalculator marketDrawdownCalculator;
    private final RiskCalculator riskCalculator;
    private final RegularBuyCalculator regularBuyCalculator;
    private final AdditionalBuyCalculator additionalBuyCalculator;
    private final RebuyCalculator rebuyCalculator;
    private final FinalActionCalculator finalActionCalculator;
    private final MarketCalendarService marketCalendarService;

    @Override
    @Transactional(readOnly = true)
    public StockAnalysisResult analyze(StockAnalysisInput input) {
        BasicStockMetrics metrics = basicStockMetricsCalculator.calculate(
                input.quantity(), input.avgPrice(), input.currentPrice(),
                input.accountTotalAmount(), input.targetWeight());

        WeightResult weight = weightCalculator.calculate(
                metrics.marketValue(), input.accountTotalAmount(), input.targetWeight(),
                new BigDecimal("0.20"));
        BigDecimal marketDrawdownRate;
        MarketPhase marketPhase;
        if (input.currentMarketIndex() != null && input.recentPeakIndex() != null) {
            var drawdown = marketDrawdownCalculator.calculate(
                    input.currentMarketIndex(), input.recentPeakIndex());
            marketDrawdownRate = drawdown.drawdownRate();
            marketPhase = drawdown.marketPhase();
        } else {
            marketDrawdownRate = input.marketReturnRate() == null
                    ? BigDecimal.ZERO : input.marketReturnRate();
            marketPhase = marketPhaseCalculator.calculate(marketDrawdownRate);
        }

        boolean individualStock = input.individualStock();
        boolean highRiskProduct = input.leveragedProduct()
                || input.thematicEtf()
                || "THEME".equalsIgnoreCase(input.stockGrade());
        RiskResult risk = riskCalculator.calculate(new RiskInput(
                individualStock, highRiskProduct, metrics.stockReturnRate(),
                weight.status(), marketPhase, input.accumulationPaused()));

        RegularBuySignal regularBuy = regularBuyCalculator.calculate(
                LocalDate.now(), marketCalendarService.isMarketOpen(LocalDate.now(), input.marketCode()),
                input.accumulationCycle(), input.accumulationWeekDays(), input.accumulationMonthDay(),
                input.accumulationPaused(),
                weight.status(), risk.level(), input.availableCash(), new BigDecimal("10000"));

        BigDecimal additionalBuyAmount = additionalBuyCalculator.calculate(
                metrics.stockReturnRate(), marketPhase, weight.status(), risk.level(),
                input.accumulationPaused(), input.fundamentalDamaged(),
                new BigDecimal("100000"), input.availableCash());

        RebuySignal rebuy = rebuyCalculator.calculate(
                metrics.stockReturnRate(), input.aboveMa5(), input.aboveMa20(),
                weight.status(), risk.level(), marketPhase, input.benchmarkStable(),
                input.existingRiskResolved(), input.rebuyEligible());

        FinalAction finalAction = finalActionCalculator.calculate(
                risk.level(), weight.status(), additionalBuyAmount, regularBuy, rebuy);

        return new StockAnalysisResult(
                input.stockCode(),
                metrics.purchaseAmount(),
                metrics.marketValue(),
                metrics.profitAmount(),
                metrics.stockReturnRate(),
                metrics.currentWeight(),
                input.targetWeight() == null ? BigDecimal.ZERO : input.targetWeight(),
                weight.maximumWeight(),
                metrics.weightDifference(),
                weight.status(),
                marketDrawdownRate,
                marketPhase,
                risk.score(),
                risk.level(),
                risk.reasons(),
                regularBuy,
                additionalBuyAmount,
                rebuy,
                finalAction);
    }
}









