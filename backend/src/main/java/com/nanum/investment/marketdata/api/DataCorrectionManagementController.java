package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.DataCorrectionManagementService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/data-corrections")
public class DataCorrectionManagementController {
  private final DataCorrectionManagementService service;
  public DataCorrectionManagementController(DataCorrectionManagementService service) { this.service = service; }

  @GetMapping
  public List<Map<String, Object>> corrections(@RequestParam(defaultValue = "100") int limit) {
    return service.corrections(limit);
  }
  @GetMapping("/recalculations")
  public List<Map<String, Object>> recalculations(@RequestParam(defaultValue = "PENDING") String status,
      @RequestParam(defaultValue = "100") int limit) { return service.recalculations(status, limit); }
  @PostMapping("/recalculations/{id}/status")
  public ResponseEntity<Void> transition(@PathVariable long id, @RequestParam String status,
      @RequestParam(required = false) String error) {
    service.transition(id, status, error);
    return ResponseEntity.noContent().build();
  }
}
