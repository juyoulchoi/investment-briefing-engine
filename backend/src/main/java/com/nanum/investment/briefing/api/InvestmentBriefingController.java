package com.nanum.investment.briefing.api;

import com.nanum.investment.briefing.application.InvestmentBriefingService;
import com.nanum.investment.briefing.domain.TbInvestmentBriefing;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/investment-briefings")
@io.swagger.v3.oas.annotations.tags.Tag(name = "브리핑", description = "투자 브리핑 조회 및 생성 API")
public class InvestmentBriefingController {
  private final InvestmentBriefingService service;

  public InvestmentBriefingController(InvestmentBriefingService service) {
    this.service = service;
  }

  @GetMapping
  @io.swagger.v3.oas.annotations.Operation(summary = "투자 브리핑 전체 조회")
  public List<TbInvestmentBriefing> findAll() {
    return service.findAll();
  }

  @GetMapping("/{briefingId}")
  @io.swagger.v3.oas.annotations.Operation(summary = "투자 브리핑 단건 조회")
  public TbInvestmentBriefing findById(@PathVariable Long briefingId) {
    return service.findById(briefingId).orElseThrow(() -> notFound("브리핑을 찾을 수 없습니다."));
  }

  @GetMapping("/by-date")
  @io.swagger.v3.oas.annotations.Operation(summary = "날짜별 투자 브리핑 조회")
  public TbInvestmentBriefing findByDate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate briefingDate) {
    return service.findByDate(briefingDate).orElseThrow(() -> notFound("해당 일자의 브리핑이 없습니다."));
  }

  @PostMapping
  @io.swagger.v3.oas.annotations.Operation(summary = "투자 브리핑 생성")
  @ResponseStatus(HttpStatus.CREATED)
  public TbInvestmentBriefing create(@RequestBody TbInvestmentBriefing briefing) {
    briefing.setBriefingId(null);
    return service.save(briefing);
  }

  @PutMapping("/{briefingId}")
  @io.swagger.v3.oas.annotations.Operation(summary = "투자 브리핑 수정")
  public TbInvestmentBriefing update(
      @PathVariable Long briefingId, @RequestBody TbInvestmentBriefing briefing) {
    service.findById(briefingId).orElseThrow(() -> notFound("브리핑을 찾을 수 없습니다."));
    briefing.setBriefingId(briefingId);
    return service.save(briefing);
  }

  @DeleteMapping("/{briefingId}")
  @io.swagger.v3.oas.annotations.Operation(summary = "투자 브리핑 삭제")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long briefingId) {
    service.findById(briefingId).orElseThrow(() -> notFound("브리핑을 찾을 수 없습니다."));
    service.delete(briefingId);
  }

  private ResponseStatusException notFound(String message) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
  }
}
