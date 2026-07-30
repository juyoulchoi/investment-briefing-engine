package com.nanum.investment.service;

import org.springframework.jdbc.core.simple.JdbcClient; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal; import java.time.LocalDate;

@Service public class CashReserveService {
 private final JdbcClient jdbc; public CashReserveService(JdbcClient jdbc){this.jdbc=jdbc;}
 @Transactional public Result saveReducedBuyAmount(Long accountId,Long regularBuyId,LocalDate date,BigDecimal amount,String idempotencyKey){return transact(accountId,regularBuyId,date,amount,idempotencyKey,true,"REG_BUY_SAVE","정기매수 감액분 적립");}
 @Transactional public Result useForAdditionalBuy(Long accountId,LocalDate date,BigDecimal amount,String idempotencyKey,Long referenceId){return transact(accountId,null,date,amount,idempotencyKey,false,"ADD_BUY_USE","추가매수 대기현금 사용",referenceId);}
 private Result transact(Long accountId,Long regularBuyId,LocalDate date,BigDecimal amount,String key,boolean incoming,String type,String reason){return transact(accountId,regularBuyId,date,amount,key,incoming,type,reason,regularBuyId);}
 private Result transact(Long accountId,Long regularBuyId,LocalDate date,BigDecimal amount,String key,boolean incoming,String type,String reason,Long referenceId){
  if(amount==null||amount.signum()<=0)throw new IllegalArgumentException("거래금액은 0보다 커야 합니다."); if(key==null||key.isBlank())throw new IllegalArgumentException("멱등성 키가 필요합니다.");
  if(jdbc.sql("SELECT EXISTS(SELECT 1 FROM \"TB_CASH_HIS\" WHERE \"IDEMP_KEY\"=:key)").param("key",key).query(Boolean.class).single()) return current(accountId,true);
  BigDecimal before=jdbc.sql("SELECT \"RSV_AMT\" FROM \"TB_CASH_RSV\" WHERE \"ACCT_ID\"=:accountId FOR UPDATE").param("accountId",accountId).query(BigDecimal.class).optional().orElseThrow(()->new IllegalArgumentException("계좌 대기현금 원장이 없습니다."));
  BigDecimal after=incoming?before.add(amount):before.subtract(amount); if(after.signum()<0)throw new IllegalStateException("추가매수 대기현금이 부족합니다.");
  jdbc.sql("UPDATE \"TB_CASH_RSV\" SET \"RSV_AMT\"=:after,\"ACCUM_AMT\"=\"ACCUM_AMT\"+:accum,\"USED_AMT\"=\"USED_AMT\"+:used,\"LAST_TX_DT\"=:date,\"VER_NO\"=\"VER_NO\"+1,\"UPD_DTTM\"=CURRENT_TIMESTAMP WHERE \"ACCT_ID\"=:accountId")
   .param("after",after).param("accum",incoming?amount:BigDecimal.ZERO).param("used",incoming?BigDecimal.ZERO:amount).param("date",date).param("accountId",accountId).update();
  jdbc.sql("UPDATE \"TB_ACCT\" SET \"RSV_CASH_AMT\"=:after,\"UPD_DTTM\"=CURRENT_TIMESTAMP WHERE \"ACCT_ID\"=:accountId").param("after",after).param("accountId",accountId).update();
  jdbc.sql("INSERT INTO \"TB_CASH_HIS\"(\"ACCT_ID\",\"REG_BUY_ID\",\"TX_DT\",\"CASH_TP\",\"TX_TP\",\"IN_OUT_TP\",\"TX_AMT\",\"BAL_BFR_AMT\",\"BAL_AFT_AMT\",\"IDEMP_KEY\",\"REF_TBL_NM\",\"REF_ID\",\"TX_RSN\",\"CRT_USR_ID\") VALUES(:accountId,:regularBuyId,:date,'RESERVE',:type,:direction,:amount,:before,:after,:key,:refTable,:referenceId,:reason,'SYSTEM')")
   .param("accountId",accountId).param("regularBuyId",regularBuyId).param("date",date).param("type",type).param("direction",incoming?"IN":"OUT").param("amount",amount).param("before",before).param("after",after).param("key",key).param("refTable",incoming?"TB_REG_BUY":"TB_ADD_BUY").param("referenceId",referenceId).param("reason",reason).update();
  return new Result(before,after,false);
 }
 private Result current(Long accountId,boolean duplicate){BigDecimal balance=jdbc.sql("SELECT \"RSV_AMT\" FROM \"TB_CASH_RSV\" WHERE \"ACCT_ID\"=:accountId").param("accountId",accountId).query(BigDecimal.class).single(); return new Result(balance,balance,duplicate);}
 public record Result(BigDecimal balanceBefore,BigDecimal balanceAfter,boolean duplicate){}
}
