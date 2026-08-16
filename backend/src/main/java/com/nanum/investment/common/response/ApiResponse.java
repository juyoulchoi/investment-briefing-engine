package com.nanum.investment.common.response;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
    boolean success, T data, ErrorResponse error, OffsetDateTime timestamp, String traceId) {
  public static <T> ApiResponse<T> success(T data, String traceId) {
    return new ApiResponse<>(true, data, null, OffsetDateTime.now(), traceId);
  }

  public static <T> ApiResponse<T> failure(ErrorResponse error, String traceId) {
    return new ApiResponse<>(false, null, error, OffsetDateTime.now(), traceId);
  }
}
