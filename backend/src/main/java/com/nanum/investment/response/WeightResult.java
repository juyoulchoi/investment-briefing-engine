package com.nanum.investment.response;

import com.nanum.investment.domain.WeightStatus;
import java.math.BigDecimal;

public record WeightResult(
        BigDecimal currentWeight,
        BigDecimal targetWeight,
        BigDecimal maximumWeight,
        WeightStatus status
) {
}
