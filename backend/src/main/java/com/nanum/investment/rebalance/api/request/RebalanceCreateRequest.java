package com.nanum.investment.rebalance.api.request;

import com.nanum.investment.rebalance.domain.RebalanceType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RebalanceCreateRequest(
    @NotNull LocalDate baseDate,
    @NotNull Long accountId,
    @NotNull RebalanceType rebalanceType,
    @DecimalMin("0") BigDecimal newCashAmount,
    boolean forceRecalculate,
    @NotNull @Min(1) Integer ruleVersionNumber) {}
