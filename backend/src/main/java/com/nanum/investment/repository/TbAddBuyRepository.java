package com.nanum.investment.repository;

import com.nanum.investment.domain.TbAddBuy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TbAddBuyRepository extends JpaRepository<TbAddBuy, Long> {
    List<TbAddBuy> findAllByBaseDateAndAccount_AccountIdAndEligibleYnOrderByPriorityNumberAsc(
            LocalDate baseDate, Long accountId, String eligibleYn);
}
