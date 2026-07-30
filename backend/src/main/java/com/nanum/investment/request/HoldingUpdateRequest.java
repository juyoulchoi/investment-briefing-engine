package com.nanum.investment.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record HoldingUpdateRequest(
    @NotNull @DecimalMin(value="0", inclusive=true) BigDecimal holdingQuantity,
    @NotNull @DecimalMin(value="0", inclusive=true) BigDecimal averagePrice
) {}
