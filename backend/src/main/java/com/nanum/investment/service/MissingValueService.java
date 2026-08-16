package com.nanum.investment.service;

import com.nanum.investment.domain.MissingValuePolicy;
import org.springframework.stereotype.Service;

@Service
public class MissingValueService {
  public <T> T resolve(T current, T last, T defaultValue, MissingValuePolicy policy) {
    if (current != null) return current;
    return switch (policy) {
      case IGNORE -> null;
      case LAST_VALUE -> last;
      case DEFAULT_VALUE -> defaultValue;
      case FAIL -> throw new IllegalStateException("필수 시장데이터가 없습니다.");
    };
  }
}
