package com.nanum.investment.repository;

import com.nanum.investment.domain.TbAddBuy;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbAddBuyRepository extends JpaRepository<TbAddBuy, Long> {
  List<TbAddBuy> findAllByBaseDateAndAccount_AccountIdAndEligibleYnOrderByPriorityNumberAsc(
      LocalDate baseDate, Long accountId, String eligibleYn);
}
