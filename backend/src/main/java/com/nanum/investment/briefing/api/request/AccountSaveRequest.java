package com.nanum.investment.briefing.api.request;

import com.nanum.investment.common.domain.AccountType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AccountSaveRequest(
    @NotNull AccountType accountType,
    @Size(max = 30) String brokerCode,
    @Size(max = 100) String brokerName,
    @Size(max = 50) String maskedAccountNumber,
    @NotBlank @Size(max = 10) String baseCurrencyCode,
    @NotNull @DecimalMin("0") BigDecimal cashAmount,
    @NotNull @DecimalMin("0") BigDecimal reservedCashAmount,
    @DecimalMin("0") @DecimalMax("100") BigDecimal targetCashWeight,
    @NotNull @Min(0) Integer displaySequence,
    @Pattern(regexp = "[YN]") String useYn) {}
