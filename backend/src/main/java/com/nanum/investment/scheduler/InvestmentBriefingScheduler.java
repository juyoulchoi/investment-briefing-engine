package com.nanum.investment.scheduler;

import com.nanum.investment.service.InvestmentBriefingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvestmentBriefingScheduler {
    private final InvestmentBriefingService briefingService;

    @Scheduled(
            cron = "${investment.briefing.scheduler.cron:0 30 7 * * MON-FRI}",
            zone = "${investment.briefing.scheduler.zone:Asia/Seoul}")
    public void generateDailyBriefing() {
        try {
            Long briefingId = briefingService.generateAndSave();
            log.info("일일 투자 브리핑 생성 완료. briefingId={}", briefingId);
        } catch (Exception e) {
            log.error("일일 투자 브리핑 생성 실패", e);
        }
    }
}
