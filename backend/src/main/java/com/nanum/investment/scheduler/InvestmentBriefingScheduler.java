package com.nanum.investment.scheduler;

import com.nanum.investment.marketdata.BriefingRefreshResult;
import com.nanum.investment.marketdata.BriefingRefreshService;
import com.nanum.investment.service.InvestmentBriefingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvestmentBriefingScheduler {
    private final BriefingRefreshService refreshService;
    private final InvestmentBriefingService briefingService;

    @Scheduled(
            cron = "${investment.briefing.scheduler.cron:0 30 8 * * MON-FRI}",
            zone = "${investment.briefing.scheduler.zone:Asia/Seoul}")
    public void generateDailyBriefing() {
        try {
            BriefingRefreshResult refresh = refreshService.refresh();
            if (!refresh.success()) {
                log.error("오전 투자 브리핑 파이프라인 중단. completedSteps={}, failures={}",
                        refresh.completedSteps(), refresh.failures());
                return;
            }
            Long briefingId = briefingService.generateAndSave();
            log.info("오전 투자 브리핑 파이프라인 완료. baseDate={}, completedSteps={}, briefingId={}",
                    refresh.baseDate(), refresh.completedSteps(), briefingId);
        } catch (Exception e) {
            log.error("오전 투자 브리핑 파이프라인 실패", e);
        }
    }
}
