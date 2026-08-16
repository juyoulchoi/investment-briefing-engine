package com.nanum.investment.briefing.domain.calculation;

import java.util.List;

public record CalculationResult<T>(T value, List<CalculationReason> reasons) {}
