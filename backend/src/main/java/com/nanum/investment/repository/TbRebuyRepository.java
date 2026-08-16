package com.nanum.investment.repository;

import com.nanum.investment.domain.RebuySignal;
import com.nanum.investment.domain.TbRebuy;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbRebuyRepository extends JpaRepository<TbRebuy, Long> {
  List<TbRebuy> findAllByBaseDateAndAccount_AccountIdAndRebuySignalOrderByPriorityNumberAsc(
      LocalDate baseDate, Long accountId, RebuySignal rebuySignal);
}
