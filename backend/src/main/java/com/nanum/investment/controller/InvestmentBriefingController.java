package com.nanum.investment.controller;

import com.nanum.investment.domain.TbInvestmentBriefing;
import com.nanum.investment.service.InvestmentBriefingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/investment-briefings")
public class InvestmentBriefingController {
    private final InvestmentBriefingService service;

    public InvestmentBriefingController(InvestmentBriefingService service) {
        this.service = service;
    }

    @GetMapping
    public List<TbInvestmentBriefing> findAll() {
        return service.findAll();
    }

    @GetMapping("/{briefingId}")
    public TbInvestmentBriefing findById(@PathVariable Long briefingId) {
        return service.findById(briefingId)
                .orElseThrow(() -> notFound("브리핑을 찾을 수 없습니다."));
    }

    @GetMapping("/by-date")
    public TbInvestmentBriefing findByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate briefingDate) {
        return service.findByDate(briefingDate)
                .orElseThrow(() -> notFound("해당 일자의 브리핑이 없습니다."));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TbInvestmentBriefing create(@RequestBody TbInvestmentBriefing briefing) {
        briefing.setBriefingId(null);
        return service.save(briefing);
    }

    @PutMapping("/{briefingId}")
    public TbInvestmentBriefing update(@PathVariable Long briefingId,
            @RequestBody TbInvestmentBriefing briefing) {
        service.findById(briefingId).orElseThrow(() -> notFound("브리핑을 찾을 수 없습니다."));
        briefing.setBriefingId(briefingId);
        return service.save(briefing);
    }

    @DeleteMapping("/{briefingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long briefingId) {
        service.findById(briefingId).orElseThrow(() -> notFound("브리핑을 찾을 수 없습니다."));
        service.delete(briefingId);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
