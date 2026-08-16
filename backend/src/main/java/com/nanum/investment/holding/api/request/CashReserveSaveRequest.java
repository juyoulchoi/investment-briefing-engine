package com.nanum.investment.holding.api.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CashReserveSaveRequest(
    @NotNull Long accountId,
    String regularBuyAccountType,
    String regularBuyStockCode,
    @NotNull LocalDate transactionDate,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
    @NotBlank @Size(max = 100) String idempotencyKey) {}
