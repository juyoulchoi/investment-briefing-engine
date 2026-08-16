package com.nanum.investment.request;

import com.nanum.investment.domain.BuyCycle;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RegularBuySaveRequest(
    @NotNull Long accountId,
    @NotNull Long stockId,
    @NotNull BuyCycle buyCycle,
    @Pattern(regexp = "MON|TUE|WED|THU|FRI") String buyDayCode,
    @Min(1) @Max(31) Integer buyDayNumber,
    @NotNull @DecimalMin("0") BigDecimal minimumBuyAmount,
    @Pattern(regexp = "[YN]") String userPauseYn,
    @Pattern(regexp = "[YN]") String autoCalculateYn) {}
