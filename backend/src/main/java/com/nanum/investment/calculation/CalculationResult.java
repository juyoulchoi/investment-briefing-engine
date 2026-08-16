package com.nanum.investment.calculation;

import java.util.List;

public record CalculationResult<T>(T value, List<CalculationReason> reasons) {}
