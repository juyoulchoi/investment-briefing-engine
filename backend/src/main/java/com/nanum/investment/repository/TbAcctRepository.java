package com.nanum.investment.repository;
import com.nanum.investment.domain.*; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface TbAcctRepository extends JpaRepository<TbAcct,Long>{ Optional<TbAcct> findByAccountTypeAndDeleteYn(AccountType accountType,String deleteYn); List<TbAcct> findAllByUseYnAndDeleteYnOrderByDisplaySequenceAsc(String useYn,String deleteYn); }
