package com.nanum.investment.briefing.application.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.briefing.api.response.BriefingItemResponse;
import com.nanum.investment.briefing.api.response.InvestmentBriefingResponse;
import com.nanum.investment.briefing.application.BriefingAiClient;
import com.nanum.investment.briefing.application.BriefingItemCatalog;
import com.nanum.investment.briefing.application.BriefingValidationService;
import com.nanum.investment.briefing.domain.BriefingScopeType;
import com.nanum.investment.briefing.domain.BriefingStatus;
import com.nanum.investment.briefing.domain.BriefingType;
import com.nanum.investment.briefing.domain.TbBrfDtl;
import com.nanum.investment.briefing.domain.TbInvBrf;
import com.nanum.investment.briefing.infrastructure.repository.TbBrfDtlRepository;
import com.nanum.investment.briefing.infrastructure.repository.TbInvBrfRepository;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InvestmentBriefingServiceImplTest {
  private static final List<String> CODES =
      BriefingItemCatalog.ITEMS.stream().map(BriefingItemCatalog.Item::code).toList();

  @Test
  void validatesStoresAndPublishesStructuredBriefing() {
    TbInvBrfRepository briefings = mock(TbInvBrfRepository.class);
    TbBrfDtlRepository details = mock(TbBrfDtlRepository.class);
    BriefingAiClient ai = mock(BriefingAiClient.class);
    BriefingValidationService validation = mock(BriefingValidationService.class);
    LocalDate date = LocalDate.of(2026, 8, 7);
    List<BriefingItemResponse> items =
        CODES.stream()
            .map(
                code -> new BriefingItemResponse(code, code + " 요약", code + " 내용", "NORMAL", false))
            .toList();
    when(ai.generateBriefing(BriefingType.DAILY))
        .thenReturn(
            new InvestmentBriefingResponse(
                date, "DAILY", "오늘의 투자 브리핑", new ObjectMapper().createObjectNode(), items));
    TbInvBrf briefing =
        TbInvBrf.builder()
            .brfId(10L)
            .baseDate(date)
            .title("원천데이터")
            .briefingStatus(BriefingStatus.READY)
            .build();
    when(briefings
            .findTopByBaseDateAndBriefingTypeAndScopeTypeAndLatestYnOrderByCalculationSequenceDesc(
                date, BriefingType.DAILY, BriefingScopeType.GLOBAL, "Y"))
        .thenReturn(Optional.of(briefing));

    Long id =
        new InvestmentBriefingServiceImpl(briefings, details, ai, validation).generateAndSave();

    assertThat(id).isEqualTo(10L);
    assertThat(briefing.getBriefingStatus()).isEqualTo(BriefingStatus.PUBLISHED);
    assertThat(briefing.getPublishedYn()).isEqualTo("Y");
    assertThat(briefing.getPublishedDateTime()).isNotNull();
    assertThat(briefing.getSummaryText()).isEqualTo("CONCLUSION 요약");
    verify(validation).validate(eq(10L), eq(BriefingType.DAILY), any());
    verify(details).deleteByBrfId(10L);
    verify(details).flush();
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<TbBrfDtl>> saved = ArgumentCaptor.forClass(List.class);
    verify(details).saveAll(saved.capture());
    assertThat(saved.getValue()).hasSize(15);
    assertThat(saved.getValue().getLast().getItemCd()).isEqualTo("CONCLUSION");
    verify(briefings).save(briefing);
  }

  @Test
  void rejectsBriefingWhenAiDateHasNoMatchingRawData() {
    TbInvBrfRepository briefings = mock(TbInvBrfRepository.class);
    TbBrfDtlRepository details = mock(TbBrfDtlRepository.class);
    BriefingAiClient ai = mock(BriefingAiClient.class);
    BriefingValidationService validation = mock(BriefingValidationService.class);
    LocalDate aiDate = LocalDate.of(2026, 8, 8);
    List<BriefingItemResponse> items =
        CODES.stream()
            .map(code -> new BriefingItemResponse(code, "요약", "내용", "NORMAL", false))
            .toList();
    when(ai.generateBriefing(BriefingType.DAILY))
        .thenReturn(
            new InvestmentBriefingResponse(
                aiDate, "DAILY", "잘못된 기준일", new ObjectMapper().createObjectNode(), items));
    when(briefings
            .findTopByBaseDateAndBriefingTypeAndScopeTypeAndLatestYnOrderByCalculationSequenceDesc(
                aiDate, BriefingType.DAILY, BriefingScopeType.GLOBAL, "Y"))
        .thenReturn(Optional.empty());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new InvestmentBriefingServiceImpl(briefings, details, ai, validation)
                    .generateAndSave())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("원천 브리핑");
    verifyNoInteractions(details);
  }
}
