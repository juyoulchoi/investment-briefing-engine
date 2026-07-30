package com.nanum.investment.repository;
import com.nanum.investment.domain.TbCashRsv; import org.springframework.data.jpa.repository.*; import jakarta.persistence.LockModeType; import java.util.*;
public interface TbCashRsvRepository extends JpaRepository<TbCashRsv,Long>{ Optional<TbCashRsv> findByAccount_AccountId(Long accountId); @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<TbCashRsv> findWithLockByAccount_AccountId(Long accountId); }
