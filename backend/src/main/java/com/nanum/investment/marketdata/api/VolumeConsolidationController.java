package com.nanum.investment.marketdata.api;

import com.nanum.investment.common.response.ApiResponse;
import com.nanum.investment.common.response.ErrorResponse;
import com.nanum.investment.common.web.TraceIdUtils;
import com.nanum.investment.marketdata.application.VolumeConsolidationService;
import com.nanum.investment.marketdata.domain.VolumeConsolidation.Screen;
import com.nanum.investment.marketdata.domain.VolumeConsolidation.Tracking;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v1/krx/screens/volume-consolidation")
@io.swagger.v3.oas.annotations.tags.Tag(name = "KRX 조건검색")
public class VolumeConsolidationController {
  private final VolumeConsolidationService service;

  public VolumeConsolidationController(VolumeConsolidationService service) {
    this.service = service;
  }

  @GetMapping
  @io.swagger.v3.oas.annotations.Operation(summary = "거래량 증가·횡보 후보 및 제외 사유 조회")
  public Screen screen(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate baseDate) {
    return service.screen(baseDate);
  }

  @GetMapping("/tracking")
  @io.swagger.v3.oas.annotations.Operation(summary = "선정일 박스를 고정한 돌파·재지지 판정")
  public Tracking tracking(
      @RequestParam String market,
      @RequestParam String stockCode,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectionDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOf) {
    return service.track(market, stockCode, selectionDate, asOf);
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ApiResponse<Void>> invalidParameters(HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(
            ApiResponse.failure(
                new ErrorResponse(
                    "COMMON-400-001", "필수 조회값과 날짜 형식(YYYY-MM-DD)을 확인해 주세요.", List.of()),
                TraceIdUtils.resolve(request)));
  }
}
