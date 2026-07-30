package com.nanum.investment.service.impl;

import com.nanum.investment.domain.BriefingStatus;
import com.nanum.investment.domain.TbBrfDtl;
import com.nanum.investment.domain.TbInvBrf;
import com.nanum.investment.domain.TbInvestmentBriefing;
import com.nanum.investment.repository.TbBrfDtlRepository;
import com.nanum.investment.repository.TbInvBrfRepository;
import com.nanum.investment.repository.TbInvestmentBriefingRepository;
import com.nanum.investment.response.BriefingItemResponse;
import com.nanum.investment.response.InvestmentBriefingResponse;
import com.nanum.investment.service.BriefingAiClient;
import com.nanum.investment.service.InvestmentBriefingService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class InvestmentBriefingServiceImpl implements InvestmentBriefingService {
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
    public Long generateAndSave() {
        InvestmentBriefingResponse response = briefingAiClient.generateBriefing();
        List<BriefingItemResponse> allItems = new ArrayList<>(response.items());
        allItems.add(response.finalJudgment());

        if (allItems.size() != 15) {
            throw new IllegalStateException(
                    "브리핑 항목은 종합판단 포함 15개여야 합니다. 현재 개수: " + allItems.size());
        }

        TbInvBrf briefing = TbInvBrf.builder()
                .baseDate(response.briefingDate())
                .title(response.title())
                .briefingStatus(BriefingStatus.GENERATED)
                .build();
        briefingRepository.save(briefing);

        List<TbBrfDtl> details = allItems.stream()
                .map(item -> TbBrfDtl.builder()
                        .brfId(briefing.getBrfId())
                        .itemCd(item.itemCode())
                        .itemSum(item.summary())
                        .itemCont(item.content())
                        .signalCd(item.signalCode())
                        .actYn(item.actionRequired() ? "Y" : "N")
                        .build())
                .toList();
        detailRepository.saveAll(details);
        return briefing.getBrfId();
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

