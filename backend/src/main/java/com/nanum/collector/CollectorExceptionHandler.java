package com.nanum.collector;

import com.nanum.investment.common.exception.BusinessException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CollectorExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Map<String, String>> business(BusinessException error) {
    return ResponseEntity.status(error.getErrorCode().getHttpStatus())
        .body(Map.of("code", error.getErrorCode().getCode(), "message", error.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> invalid(RuntimeException error) {
    return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
  }
}
