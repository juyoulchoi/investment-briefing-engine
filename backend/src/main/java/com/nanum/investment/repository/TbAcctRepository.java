package com.nanum.investment.repository;
import com.nanum.investment.domain.TbAcct; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface TbAcctRepository extends JpaRepository<TbAcct,Long>{ Optional<TbAcct> findByAccountCodeAndDeleteYn(String accountCode,String deleteYn); List<TbAcct> findAllByUseYnAndDeleteYnOrderByDisplaySequenceAsc(String useYn,String deleteYn); }
