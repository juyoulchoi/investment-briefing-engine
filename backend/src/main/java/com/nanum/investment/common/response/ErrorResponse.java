package com.nanum.investment.common.response;

import java.util.List;

public record ErrorResponse(String code, String message, List<ValidationError> validationErrors) {}
