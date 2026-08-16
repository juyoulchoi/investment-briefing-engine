package com.nanum.investment.external;

import java.util.List;

public record ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
  public static ValidationResult success() {
    return new ValidationResult(true, List.of(), List.of());
  }

  public static ValidationResult invalid(String error) {
    return new ValidationResult(false, List.of(error), List.of());
  }
}
