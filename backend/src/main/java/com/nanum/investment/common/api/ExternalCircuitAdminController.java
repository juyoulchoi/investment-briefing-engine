package com.nanum.investment.common.api;

import com.nanum.investment.common.infrastructure.external.CircuitBreakerSupport;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/external-circuits")
public class ExternalCircuitAdminController {
  private final CircuitBreakerSupport circuits;

  public ExternalCircuitAdminController(CircuitBreakerSupport circuits) {
    this.circuits = circuits;
  }

  @GetMapping
  @Operation(summary = "외부 공급자 Circuit Breaker 상태 조회")
  public List<Map<String, Object>> states() { return circuits.states(); }

  @PostMapping("/{key}/reset")
  @Operation(summary = "외부 공급자 Circuit Breaker 강제 해제")
  public Map<String, Object> reset(@PathVariable String key) {
    circuits.reset(key);
    return Map.of("circuitKey", key, "state", "CLOSED");
  }
}
