package com.nanum.investment.common.infrastructure.repository;

import com.nanum.investment.common.domain.AccountType;
import com.nanum.investment.common.domain.TbAcct;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbAcctRepository extends JpaRepository<TbAcct, Long> {
  Optional<TbAcct> findByAccountTypeAndDeleteYn(AccountType accountType, String deleteYn);

  List<TbAcct> findAllByDeleteYnOrderByDisplaySequenceAsc(String deleteYn);
}
