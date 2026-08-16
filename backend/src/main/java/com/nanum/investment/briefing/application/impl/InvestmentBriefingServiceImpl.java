package com.nanum.investment.briefing.application.impl;

import com.nanum.investment.briefing.api.response.BriefingItemResponse;
import com.nanum.investment.briefing.api.response.InvestmentBriefingResponse;
import com.nanum.investment.briefing.application.BriefingAiClient;
import com.nanum.investment.briefing.application.InvestmentBriefingService;
import com.nanum.investment.briefing.domain.BriefingScopeType;
import com.nanum.investment.briefing.domain.BriefingStatus;
import com.nanum.investment.briefing.domain.BriefingType;
import com.nanum.investment.briefing.domain.TbBrfDtl;
import com.nanum.investment.briefing.domain.TbInvBrf;
import com.nanum.investment.briefing.domain.TbInvestmentBriefing;
import com.nanum.investment.briefing.infrastructure.repository.TbBrfDtlRepository;
import com.nanum.investment.briefing.infrastructure.repository.TbInvBrfRepository;
import com.nanum.investment.briefing.infrastructure.repository.TbInvestmentBriefingRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class InvestmentBriefingServiceImpl implements InvestmentBriefingService {
  private static final List<String> ITEM_CODES =
      List.of(
          "US_STOCK_MKT",
          "US_BOND_MKT",
          "KR_STOCK_MKT",
          "FX_RATE_CMDTY",
          "ECON_SCHEDULE",
          "MKT_RISK",
          "MKT_PHASE",
          "REG_BUY_DEC",
          "ADD_BUY_DEC",
          "REBUY_SIG",
          "ACCT_STRATEGY",
          "HOLDING_SIGNAL",
          "TODAY_ACTION",
          "CAUTION");

  private final TbInvestmentBriefingRepository repository;
  private final TbInvBrfRepository briefingRepository;
  private final TbBrfDtlRepository detailRepository;
  private final BriefingAiClient briefingAiClient;

  public InvestmentBriefingServiceImpl(
      TbInvestmentBriefingRepository repository,
      TbInvBrfRepository briefingRepository,
      TbBrfDtlRepository detailRepository,
      BriefingAiClient briefingAiClient) {
    this.repository = repository;
    this.briefingRepository = briefingRepository;
    this.detailRepository = detailRepository;
    this.briefingAiClient = briefingAiClient;
  }

  @Override
  @Transactional
  public Long generateAndSave(BriefingType briefingType) {
    InvestmentBriefingResponse response = briefingAiClient.generateBriefing(briefingType);
    validate(response);
    TbInvBrf briefing =
        briefingRepository
            .findTopByBaseDateAndBriefingTypeAndScopeTypeAndLatestYnOrderByCalculationSequenceDesc(
                response.briefingDate(), briefingType, BriefingScopeType.GLOBAL, "Y")
            .orElseThrow(() -> new IllegalStateException("AI 응답 기준일·유형과 일치하는 원천 브리핑이 없습니다."));

    briefing.setTitle(response.title());
    briefing.setBriefingStatus(BriefingStatus.REVIEWED);
    detailRepository.deleteByBrfId(briefing.getBrfId());
    detailRepository.flush();

    List<BriefingItemResponse> allItems = new ArrayList<>(response.items());
    allItems.add(response.finalJudgment());
    detailRepository.saveAll(
        allItems.stream()
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

    briefing.setSummaryText(response.finalJudgment().summary());
    briefing.setBodyText(response.finalJudgment().content());
    briefing.setBriefingStatus(BriefingStatus.PUBLISHED);
    briefing.setPublishedYn("Y");
    briefing.setPublishedDateTime(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
    briefingRepository.save(briefing);
    return briefing.getBrfId();
  }

  private void validate(InvestmentBriefingResponse response) {
    if (response == null
        || response.briefingDate() == null
        || !StringUtils.hasText(response.title()))
      throw new IllegalStateException("브리핑 기준일과 제목이 필요합니다.");
    if (response.title().length() > 300) throw new IllegalStateException("브리핑 제목은 300자 이하여야 합니다.");
    if (response.items() == null
        || response.items().size() != 14
        || response.finalJudgment() == null)
      throw new IllegalStateException("브리핑은 일반항목 14개와 종합판단 1개여야 합니다.");
    List<String> actual = response.items().stream().map(BriefingItemResponse::itemCode).toList();
    if (!ITEM_CODES.equals(actual))
      throw new IllegalStateException("브리핑 항목 코드 또는 순서가 기준과 다릅니다: " + actual);
    if (!"FINAL_JUDGMENT".equals(response.finalJudgment().itemCode()))
      throw new IllegalStateException("종합판단 항목 코드가 FINAL_JUDGMENT가 아닙니다.");
    List<BriefingItemResponse> all = new ArrayList<>(response.items());
    all.add(response.finalJudgment());
    for (BriefingItemResponse item : all) {
      if (!StringUtils.hasText(item.summary())
          || !StringUtils.hasText(item.content())
          || !StringUtils.hasText(item.signalCode()))
        throw new IllegalStateException(item.itemCode() + " 항목에 필수 설명이 없습니다.");
      if (item.summary().length() > 1000)
        throw new IllegalStateException(item.itemCode() + " 요약이 1000자를 초과했습니다.");
      if (!Set.of("NORMAL", "WATCH", "CAUTION", "RISK").contains(item.signalCode()))
        throw new IllegalStateException(item.itemCode() + " 신호 코드가 허용 범위를 벗어났습니다.");
    }
  }

  public List<TbInvestmentBriefing> findAll() {
    return repository.findAll(Sort.by(Sort.Direction.DESC, "briefingDate"));
  }

  public Optional<TbInvestmentBriefing> findById(Long briefingId) {
    return repository.findById(briefingId);
  }

  public Optional<TbInvestmentBriefing> findByDate(LocalDate briefingDate) {
    return repository.findByBriefingDate(briefingDate);
  }

  @Transactional
  public TbInvestmentBriefing save(TbInvestmentBriefing briefing) {
    return repository.save(briefing);
  }

  @Transactional
  public void delete(Long briefingId) {
    repository.deleteById(briefingId);
  }
}
