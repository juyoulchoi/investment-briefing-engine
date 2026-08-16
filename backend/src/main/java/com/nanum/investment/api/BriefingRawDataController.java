package com.nanum.investment.api;

import com.nanum.investment.service.*;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/briefings/raw-data")
public class BriefingRawDataController {
  private final BriefingRawDataService rawData;

  public BriefingRawDataController(BriefingRawDataService rawData) {
    this.rawData = rawData;
  }

  @PostMapping("/generate")
  public BriefingRawDataResult generate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return rawData.generate(baseDate);
  }

  @GetMapping("/{briefingId}")
  public Map<String, Object> find(@PathVariable Long briefingId) {
    return rawData.find(briefingId);
  }
}
