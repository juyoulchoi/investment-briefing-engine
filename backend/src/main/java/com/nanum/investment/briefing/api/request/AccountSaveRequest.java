package com.nanum.investment.briefing.api.request;

import com.nanum.investment.common.domain.AccountType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AccountSaveRequest(
    @NotNull AccountType accountType,
    @NotNull @DecimalMin("0") BigDecimal cashAmount,
    @NotNull @DecimalMin("0") BigDecimal reservedCashAmount,
    @DecimalMin("0") @DecimalMax("100") BigDecimal targetCashWeight,
    @NotNull @Min(0) Integer displaySequence) {}
