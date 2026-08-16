package com.nanum.investment.briefing.infrastructure.repository;

import com.nanum.investment.briefing.domain.BriefingScopeType;
import com.nanum.investment.briefing.domain.BriefingType;
import com.nanum.investment.briefing.domain.TbInvBrf;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbInvBrfRepository extends JpaRepository<TbInvBrf, Long> {
  Optional<TbInvBrf> findTopByBaseDateAndLatestYnOrderByCalculationSequenceDesc(
      LocalDate baseDate, String latestYn);

  Optional<TbInvBrf>
      findTopByBaseDateAndBriefingTypeAndScopeTypeAndLatestYnOrderByCalculationSequenceDesc(
          LocalDate baseDate,
          BriefingType briefingType,
          BriefingScopeType scopeType,
          String latestYn);
}
