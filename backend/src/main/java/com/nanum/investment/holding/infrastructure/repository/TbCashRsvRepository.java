package com.nanum.investment.holding.infrastructure.repository;

import com.nanum.investment.holding.domain.TbCashRsv;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface TbCashRsvRepository extends JpaRepository<TbCashRsv, Long> {
  Optional<TbCashRsv> findByAccount_AccountId(Long accountId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<TbCashRsv> findWithLockByAccount_AccountId(Long accountId);
}
