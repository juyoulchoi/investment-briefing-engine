package com.nanum.investment.calculation;

import com.nanum.investment.domain.SentimentPhase;
import org.springframework.stereotype.Component;
import java.math.*;
import java.util.List;

@Component
public class MarketSentimentCalculator {
    public Result calculate(MarketSentimentInput i) {
        if (i == null)
            throw new IllegalArgumentException("시장심리 입력은 필수입니다.");
        BigDecimal score = b("100").subtract(n(i.newsFearScore())).multiply(b("0.20"))
                .add(b("100").subtract(n(i.aiFatigueScore())).multiply(b("0.10")))
                .add(n(i.earningsConfidenceScore()).multiply(b("0.20"))).add(n(i.liquidityScore()).multiply(b("0.10")))
                .add(n(i.flowScore()).multiply(b("0.15"))).add(b("100").subtract(n(i.exchangePressureScore())).multiply(b("0.10")))
                .add(b("100").subtract(n(i.ratePressureScore())).multiply(b("0.05")))
                .add(b("100").subtract(n(i.volatilityPressureScore())).multiply(b("0.10")))
                .max(BigDecimal.ZERO).min(b("100")).setScale(4, RoundingMode.HALF_UP);
        SentimentPhase phase = score.compareTo(b("80")) >= 0 ? SentimentPhase.GREED
                : score.compareTo(b("65")) >= 0 ? SentimentPhase.OPTIMISM
                        : score.compareTo(b("45")) >= 0 ? SentimentPhase.NEUTRAL
                                : score.compareTo(b("30")) >= 0 ? SentimentPhase.FATIGUE
                                        : score.compareTo(b("15")) >= 0 ? SentimentPhase.FEAR : SentimentPhase.PANIC;
        return new Result(score, phase, n(i.dataConfidenceRate()).max(BigDecimal.ZERO).min(b("100")),
                List.of(new CalculationReason("SENTIMENT", "시장심리 " + phase)));
    }

    private BigDecimal n(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal b(String v) {
        return new BigDecimal(v);
    }

    public record Result(BigDecimal score, SentimentPhase phase, BigDecimal confidenceRate,
            List<CalculationReason> reasons) {
    }
}
