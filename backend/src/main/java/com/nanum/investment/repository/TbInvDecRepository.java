package com.nanum.investment.repository;

import com.nanum.investment.domain.TbInvDec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TbInvDecRepository extends JpaRepository<TbInvDec, Long> {
    Optional<TbInvDec> findFirstByBaseDateAndAccount_AccountIdAndMarketSnapshotCodeAndLatestYnOrderByCalculationSequenceDesc(
            LocalDate baseDate, Long accountId, String marketSnapshotCode, String latestYn);

    List<TbInvDec> findAllByBaseDateAndLatestYnOrderByAccount_AccountIdAsc(
            LocalDate baseDate, String latestYn);
}
