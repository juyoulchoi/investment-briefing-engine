package com.nanum.investment.common.api.response;

import com.nanum.investment.common.domain.AccountType;
import java.math.BigDecimal;

public record AccountApiResponse(
    Long accountId,
    String accountCode,
    String accountName,
    AccountType accountType,
    BigDecimal cashAmount,
    BigDecimal reservedCashAmount,
    BigDecimal targetCashWeight) {}
