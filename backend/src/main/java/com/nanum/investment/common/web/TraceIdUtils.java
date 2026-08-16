package com.nanum.investment.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public final class TraceIdUtils {
  public static final String ATTRIBUTE = "traceId";

  private TraceIdUtils() {}

  public static String resolve(HttpServletRequest request) {
    Object value = request.getAttribute(ATTRIBUTE);
    return value == null ? UUID.randomUUID().toString() : value.toString();
  }
}
