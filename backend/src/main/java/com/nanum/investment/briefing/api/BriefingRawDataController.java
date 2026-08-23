package com.nanum.investment.briefing.api;

import com.nanum.investment.briefing.application.BriefingRawDataResult;
import com.nanum.investment.briefing.application.BriefingRawDataService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/briefings/raw-data")
@io.swagger.v3.oas.annotations.tags.Tag(name = "브리핑", description = "투자 브리핑 조회 및 생성 API")
public class BriefingRawDataController {
  private final BriefingRawDataService rawData;

  public BriefingRawDataController(BriefingRawDataService rawData) {
    this.rawData = rawData;
  }

  @PostMapping("/generate")
  @io.swagger.v3.oas.annotations.Operation(summary = "브리핑 원본 데이터 스냅샷 생성")
  public BriefingRawDataResult generate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return rawData.generate(baseDate);
  }

  @GetMapping("/{briefingId}")
  @io.swagger.v3.oas.annotations.Operation(summary = "브리핑 원본 데이터 조회")
  public Map<String, Object> find(@PathVariable Long briefingId) {
    return rawData.find(briefingId);
  }
}
