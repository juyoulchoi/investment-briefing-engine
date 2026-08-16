package com.nanum.investment.briefing.infrastructure.repository;

import com.nanum.investment.briefing.domain.TbInvDec;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbInvDecRepository extends JpaRepository<TbInvDec, Long> {
  Optional<TbInvDec>
      findFirstByBaseDateAndAccount_AccountIdAndMarketSnapshotCodeAndLatestYnOrderByCalculationSequenceDesc(
          LocalDate baseDate, Long accountId, String marketSnapshotCode, String latestYn);

  List<TbInvDec> findAllByBaseDateAndLatestYnOrderByAccount_AccountIdAsc(
      LocalDate baseDate, String latestYn);
}
