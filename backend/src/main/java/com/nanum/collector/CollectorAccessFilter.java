package com.nanum.collector;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CollectorAccessFilter extends OncePerRequestFilter {
  private final boolean enabled;

  public CollectorAccessFilter(@Value("${KOFIA_SHARED_COLLECTION_ENABLED:false}") boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!enabled && !"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
      response.setStatus(409);
      response.setContentType("application/json");
      response
          .getWriter()
          .write(
              "{\"code\":\"COLLECTOR_READ_ONLY\",\"message\":\"Collection is disabled until verified data handoff\"}");
      return;
    }
    chain.doFilter(request, response);
  }
}
