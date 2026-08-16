package com.nanum.investment.briefing.api.response;

import com.nanum.investment.holding.domain.WeightStatus;
import java.math.BigDecimal;

public record WeightResult(
    BigDecimal currentWeight,
    BigDecimal targetWeight,
    BigDecimal maximumWeight,
    WeightStatus status) {}
