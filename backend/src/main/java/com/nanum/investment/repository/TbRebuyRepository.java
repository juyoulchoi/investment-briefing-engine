package com.nanum.investment.repository;

import com.nanum.investment.domain.RebuySignal;
import com.nanum.investment.domain.TbRebuy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TbRebuyRepository extends JpaRepository<TbRebuy, Long> {
    List<TbRebuy> findAllByBaseDateAndAccount_AccountIdAndRebuySignalOrderByPriorityNumberAsc(
            LocalDate baseDate, Long accountId, RebuySignal rebuySignal);
}
