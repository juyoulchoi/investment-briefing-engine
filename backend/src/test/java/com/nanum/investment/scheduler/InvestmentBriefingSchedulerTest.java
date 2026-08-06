package com.nanum.investment.scheduler;

import com.nanum.investment.marketdata.BriefingRefreshService;
import com.nanum.investment.service.WeeklyInvestmentBriefingService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InvestmentBriefingSchedulerTest {
    @Test
    void weeklyBriefingRunsOnlyStepsElevenThroughThirteenOnSundayAt0820() throws Exception {
        BriefingRefreshService daily=mock(BriefingRefreshService.class);
        WeeklyInvestmentBriefingService weekly=mock(WeeklyInvestmentBriefingService.class);
        when(weekly.generateAndSave()).thenReturn(77L);
        InvestmentBriefingScheduler scheduler=new InvestmentBriefingScheduler(daily,weekly);

        scheduler.generateWeeklyBriefing();

        verify(weekly).generateAndSave();
        verifyNoInteractions(daily);
        Scheduled scheduled=InvestmentBriefingScheduler.class.getMethod("generateWeeklyBriefing")
                .getAnnotation(Scheduled.class);
        assertThat(scheduled.cron()).isEqualTo("${investment.briefing.weekly-scheduler.cron:0 20 8 * * SUN}");
        assertThat(scheduled.zone()).isEqualTo("${investment.briefing.scheduler.zone:Asia/Seoul}");
    }
}
