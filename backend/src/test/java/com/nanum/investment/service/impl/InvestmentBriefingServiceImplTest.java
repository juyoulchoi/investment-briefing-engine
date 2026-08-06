package com.nanum.investment.service.impl;

import com.nanum.investment.domain.*;
import com.nanum.investment.repository.*;
import com.nanum.investment.response.*;
import com.nanum.investment.service.BriefingAiClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InvestmentBriefingServiceImplTest {
    private static final List<String> CODES=List.of(
            "US_STOCK_MKT","US_BOND_MKT","KR_STOCK_MKT","FX_RATE_CMDTY","ECON_SCHEDULE","MKT_RISK",
            "MKT_PHASE","REG_BUY_DEC","ADD_BUY_DEC","REBUY_SIG","ACCT_STRATEGY","HOLDING_SIGNAL",
            "TODAY_ACTION","CAUTION");

    @Test
    void validatesStoresAndPublishesStructuredBriefing(){
        TbInvestmentBriefingRepository legacy=mock(TbInvestmentBriefingRepository.class);
        TbInvBrfRepository briefings=mock(TbInvBrfRepository.class);
        TbBrfDtlRepository details=mock(TbBrfDtlRepository.class);
        BriefingAiClient ai=mock(BriefingAiClient.class);
        LocalDate date=LocalDate.of(2026,8,7);
        List<BriefingItemResponse> items=CODES.stream()
                .map(code->new BriefingItemResponse(code,code+" 요약",code+" 내용","NORMAL",false)).toList();
        when(ai.generateBriefing(BriefingType.DAILY)).thenReturn(new InvestmentBriefingResponse(date,"오늘의 투자 브리핑",items,
                new BriefingItemResponse("FINAL_JUDGMENT","종합 요약","종합 내용","WATCH",true)));
        TbInvBrf briefing=TbInvBrf.builder().brfId(10L).baseDate(date).title("원천데이터")
                .briefingStatus(BriefingStatus.READY).build();
        when(briefings.findTopByBaseDateAndBriefingTypeAndScopeTypeAndLatestYnOrderByCalculationSequenceDesc(
                date,BriefingType.DAILY,BriefingScopeType.GLOBAL,"Y"))
                .thenReturn(Optional.of(briefing));

        Long id=new InvestmentBriefingServiceImpl(legacy,briefings,details,ai).generateAndSave();

        assertThat(id).isEqualTo(10L);
        assertThat(briefing.getBriefingStatus()).isEqualTo(BriefingStatus.PUBLISHED);
        assertThat(briefing.getPublishedYn()).isEqualTo("Y");
        assertThat(briefing.getPublishedDateTime()).isNotNull();
        assertThat(briefing.getSummaryText()).isEqualTo("종합 요약");
        verify(details).deleteByBrfId(10L);
        verify(details).flush();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TbBrfDtl>> saved=ArgumentCaptor.forClass(List.class);
        verify(details).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(15);
        assertThat(saved.getValue().getLast().getItemCd()).isEqualTo("FINAL_JUDGMENT");
        verify(briefings).save(briefing);
    }

    @Test
    void rejectsBriefingWhenAiDateHasNoMatchingRawData(){
        TbInvestmentBriefingRepository legacy=mock(TbInvestmentBriefingRepository.class);
        TbInvBrfRepository briefings=mock(TbInvBrfRepository.class);
        TbBrfDtlRepository details=mock(TbBrfDtlRepository.class);
        BriefingAiClient ai=mock(BriefingAiClient.class);
        LocalDate aiDate=LocalDate.of(2026,8,8);
        List<BriefingItemResponse> items=CODES.stream()
                .map(code->new BriefingItemResponse(code,"요약","내용","NORMAL",false)).toList();
        when(ai.generateBriefing(BriefingType.DAILY)).thenReturn(new InvestmentBriefingResponse(aiDate,"잘못된 기준일",items,
                new BriefingItemResponse("FINAL_JUDGMENT","종합 요약","종합 내용","WATCH",false)));
        when(briefings.findTopByBaseDateAndBriefingTypeAndScopeTypeAndLatestYnOrderByCalculationSequenceDesc(
                aiDate,BriefingType.DAILY,BriefingScopeType.GLOBAL,"Y"))
                .thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                ()->new InvestmentBriefingServiceImpl(legacy,briefings,details,ai).generateAndSave())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("원천 브리핑");
        verifyNoInteractions(details);
    }
}
