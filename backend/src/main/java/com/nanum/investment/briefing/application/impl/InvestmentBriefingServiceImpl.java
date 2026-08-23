package com.nanum.investment.briefing.application.impl;

import com.nanum.investment.briefing.api.response.BriefingItemResponse;
import com.nanum.investment.briefing.api.response.InvestmentBriefingResponse;
import com.nanum.investment.briefing.application.BriefingAiClient;
import com.nanum.investment.briefing.application.BriefingValidationService;
import com.nanum.investment.briefing.application.InvestmentBriefingService;
import com.nanum.investment.briefing.domain.BriefingScopeType;
import com.nanum.investment.briefing.domain.BriefingStatus;
import com.nanum.investment.briefing.domain.BriefingType;
import com.nanum.investment.briefing.domain.TbBrfDtl;
import com.nanum.investment.briefing.domain.TbInvBrf;
import com.nanum.investment.briefing.infrastructure.repository.TbBrfDtlRepository;
import com.nanum.investment.briefing.infrastructure.repository.TbInvBrfRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InvestmentBriefingServiceImpl implements InvestmentBriefingService {
  private final TbInvBrfRepository briefingRepository;
  private final TbBrfDtlRepository detailRepository;
  private final BriefingAiClient briefingAiClient;
  private final BriefingValidationService validationService;

  public InvestmentBriefingServiceImpl(
      TbInvBrfRepository briefingRepository,
      TbBrfDtlRepository detailRepository,
      BriefingAiClient briefingAiClient,
      BriefingValidationService validationService) {
    this.briefingRepository = briefingRepository;
    this.detailRepository = detailRepository;
    this.briefingAiClient = briefingAiClient;
    this.validationService = validationService;
  }

  @Override
  @Transactional
  public Long generateAndSave(BriefingType briefingType) {
    InvestmentBriefingResponse response = briefingAiClient.generateBriefing(briefingType);
    TbInvBrf briefing =
        briefingRepository
            .findTopByBaseDateAndBriefingTypeAndScopeTypeAndLatestYnOrderByCalculationSequenceDesc(
                response.briefingDate(), briefingType, BriefingScopeType.GLOBAL, "Y")
            .orElseThrow(() -> new IllegalStateException("AI 응답 기준일·유형과 일치하는 원천 브리핑이 없습니다."));
    validationService.validate(briefing.getBrfId(), briefingType, response);

    briefing.setTitle(response.title());
    briefing.setBriefingStatus(BriefingStatus.REVIEWED);
    detailRepository.deleteByBrfId(briefing.getBrfId());
    detailRepository.flush();

    detailRepository.saveAll(
        response.items().stream()
            .map(
                item ->
                    TbBrfDtl.builder()
                        .brfId(briefing.getBrfId())
                        .itemCd(item.itemCode())
                        .itemSum(item.summary())
                        .itemCont(item.content())
                        .signalCd(item.signalCode())
                        .actYn(item.actionRequired() ? "Y" : "N")
                        .build())
            .toList());

    BriefingItemResponse conclusion = response.items().getLast();
    briefing.setSummaryText(conclusion.summary());
    briefing.setBodyText(conclusion.content());
    briefing.setBriefingStatus(BriefingStatus.PUBLISHED);
    briefing.setPublishedYn("Y");
    briefing.setPublishedDateTime(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
    briefingRepository.save(briefing);
    return briefing.getBrfId();
  }
}
