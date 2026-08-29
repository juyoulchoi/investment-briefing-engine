package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.CollectionJobReprocessingService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collection-reprocessing")
public class CollectionJobReprocessingController {
  private final CollectionJobReprocessingService service;
  public CollectionJobReprocessingController(CollectionJobReprocessingService service) { this.service = service; }

  @PostMapping("/{provider}/{jobId}")
  public ResponseEntity<CollectionJobReprocessingService.ReprocessingView> retry(
      @PathVariable String provider, @PathVariable UUID jobId) {
    return ResponseEntity.accepted().body(service.retry(provider, jobId));
  }

  @PostMapping("/{provider}/{jobId}/permanent-failure")
  public ResponseEntity<Void> permanentFailure(@PathVariable String provider,
      @PathVariable UUID jobId, @RequestParam(required = false) String reason) {
    service.markPermanentFailure(provider, jobId, reason);
    return ResponseEntity.noContent().build();
  }
}
