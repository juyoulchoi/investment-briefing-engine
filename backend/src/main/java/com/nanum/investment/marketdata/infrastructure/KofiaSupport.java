package com.nanum.investment.marketdata.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

final class KofiaSupport {
  private KofiaSupport() {}

  static String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception error) {
      throw new IllegalStateException(error);
    }
  }

  static String trim(String value) {
    if (value == null || value.length() <= 4000) return value;
    return value.substring(0, 4000);
  }
}
