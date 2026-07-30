package com.nanum.investment.repository;
import com.nanum.investment.domain.TbStk; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface TbStkRepository extends JpaRepository<TbStk,Long>{ Optional<TbStk> findByStockCodeAndDeleteYn(String stockCode,String deleteYn); Optional<TbStk> findByTickerAndMarketCodeAndDeleteYn(String ticker,String marketCode,String deleteYn); List<TbStk> findAllByUseYnAndDeleteYnOrderByStockNameAsc(String useYn,String deleteYn); }
