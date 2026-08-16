package com.nanum.investment.repository;

import com.nanum.investment.domain.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbAcctRepository extends JpaRepository<TbAcct, Long> {
  Optional<TbAcct> findByAccountTypeAndDeleteYn(AccountType accountType, String deleteYn);

  List<TbAcct> findAllByUseYnAndDeleteYnOrderByDisplaySequenceAsc(String useYn, String deleteYn);
}
