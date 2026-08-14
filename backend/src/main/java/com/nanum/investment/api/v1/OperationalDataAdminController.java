package com.nanum.investment.api.v1;

import com.nanum.investment.common.exception.*;
import com.nanum.investment.common.response.ApiResponse;
import com.nanum.investment.common.web.TraceIdUtils;
import com.nanum.investment.domain.*;
import com.nanum.investment.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/operations")
@Transactional(readOnly=true)
public class OperationalDataAdminController {
 private final TbHoldRepository holdings; private final TbRegBuyRepository regularBuys; private final TbCashRsvRepository cashReserves; private final TbAcctRepository accounts; private final TbStkRepository stocks; private final JdbcClient jdbc;
 public OperationalDataAdminController(TbHoldRepository holdings,TbRegBuyRepository regularBuys,TbCashRsvRepository cashReserves,TbAcctRepository accounts,TbStkRepository stocks,JdbcClient jdbc){this.holdings=holdings;this.regularBuys=regularBuys;this.cashReserves=cashReserves;this.accounts=accounts;this.stocks=stocks;this.jdbc=jdbc;}
 public record HoldingRequest(@NotNull Long accountId,@NotNull Long stockId,@NotNull @DecimalMin("0") BigDecimal holdingQuantity,@NotNull @DecimalMin("0") BigDecimal averagePrice,@NotNull @DecimalMin("0") BigDecimal exchangeRate,@DecimalMin("0") @DecimalMax("100") BigDecimal targetWeight,@NotNull HoldingStatus holdingStatus,@Size(max=1000) String memo,@Pattern(regexp="[YN]") String useYn){}
 public record HoldingRow(Long holdingId,Long accountId,AccountType accountType,Long stockId,String stockCode,String stockName,BigDecimal holdingQuantity,BigDecimal averagePrice,BigDecimal currentPrice,BigDecimal exchangeRate,BigDecimal evaluationAmount,BigDecimal profitLossRate,BigDecimal targetWeight,HoldingStatus holdingStatus,String memo,String useYn){}
 public record RegularBuyRequest(@NotNull Long accountId,@NotNull Long stockId,@NotNull BuyCycle buyCycle,@Pattern(regexp="(MON|TUE|WED|THU|FRI)(,(MON|TUE|WED|THU|FRI))*") String buyDayCode,@Min(1) @Max(31) Integer buyDayNumber,@Pattern(regexp="([1-9]|[12][0-9]|3[01])(,([1-9]|[12][0-9]|3[01]))*") String buyDayNumbers,@NotNull @Pattern(regexp="AMOUNT|QUANTITY") String buyBasis,@NotNull @DecimalMin("0") BigDecimal minimumBuyAmount,@DecimalMin("0") BigDecimal maximumBuyAmount,@DecimalMin("0") BigDecimal baseBuyQuantity,@DecimalMin("0") BigDecimal buyQuantity,@NotNull @DecimalMin("0") BigDecimal maximumMultiplier,@NotNull RegularBuyStatus buyStatus,@Size(max=500) String pauseReason,@Pattern(regexp="[YN]") String userPauseYn,@Pattern(regexp="[YN]") String autoCalculateYn){}
 public record RegularBuyRow(String regularBuyKey,Long accountId,AccountType accountType,Long stockId,String stockCode,String stockName,BuyCycle buyCycle,String buyDayCode,Integer buyDayNumber,String buyDayNumbers,String buyBasis,BigDecimal minimumBuyAmount,BigDecimal maximumBuyAmount,BigDecimal baseBuyQuantity,BigDecimal buyQuantity,BigDecimal maximumMultiplier,BigDecimal recommendedBuyAmount,RegularBuyStatus buyStatus,String pauseReason,String userPauseYn,String autoCalculateYn){}
 public record CashReserveRequest(@NotNull Long accountId,@NotNull @DecimalMin("0") BigDecimal reserveAmount,@NotNull @DecimalMin("0") BigDecimal accumulatedAmount,@NotNull @DecimalMin("0") BigDecimal usedAmount,LocalDate lastTransactionDate){}
 public record CashReserveRow(Long cashReserveId,Long accountId,AccountType accountType,BigDecimal reserveAmount,BigDecimal accumulatedAmount,BigDecimal usedAmount,BigDecimal availableAmount,LocalDate lastTransactionDate,Long version){}
 @GetMapping("/holdings") public ApiResponse<List<HoldingRow>> holdings(HttpServletRequest r){return ok(holdings.findAll().stream().filter(x->"N".equals(x.getDeleteYn())).map(this::row).toList(),r);}
 @PostMapping("/holdings") @Transactional public ApiResponse<HoldingRow> createHolding(@Valid @RequestBody HoldingRequest b,HttpServletRequest r){if(holdings.findByAccount_AccountIdAndStock_StockIdAndDeleteYn(b.accountId(),b.stockId(),"N").isPresent())throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);TbHold x=new TbHold();apply(x,b);TbHold saved=holdings.save(x);createDefaultRegularBuy(saved);return ok(row(saved),r);}
 @PutMapping("/holdings/{id}") @Transactional public ApiResponse<HoldingRow> updateHolding(@PathVariable Long id,@Valid @RequestBody HoldingRequest b,HttpServletRequest r){TbHold x=holdings.findById(id).filter(v->"N".equals(v.getDeleteYn())).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));holdings.findByAccount_AccountIdAndStock_StockIdAndDeleteYn(b.accountId(),b.stockId(),"N").filter(v->!v.getHoldingId().equals(id)).ifPresent(v->{throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);});apply(x,b);return ok(row(holdings.save(x)),r);}
 @GetMapping("/regular-buys") public ApiResponse<List<Map<String,Object>>> regularBuys(HttpServletRequest r){return ok(regularBuyRows(),r);}
 @PostMapping("/regular-buys") @Transactional public ApiResponse<RegularBuyRow> createRegularBuy(@Valid @RequestBody RegularBuyRequest b,HttpServletRequest r){if(regularBuys.findByAccount_AccountIdAndStock_StockIdAndDeleteYn(b.accountId(),b.stockId(),"N").isPresent())throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);TbRegBuy x=new TbRegBuy();apply(x,b);return ok(row(regularBuys.save(x)),r);}
 @PutMapping("/regular-buys/{accountType}/{stockCode}") @Transactional public ApiResponse<RegularBuyRow> updateRegularBuy(@PathVariable AccountType accountType,@PathVariable String stockCode,@Valid @RequestBody RegularBuyRequest b,HttpServletRequest r){TbRegBuy x=regularBuys.findById(new TbRegBuyId(accountType,stockCode)).filter(v->"N".equals(v.getDeleteYn())).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));TbAcct targetAccount=account(b.accountId());TbStk targetStock=stock(b.stockId());if(targetAccount.getAccountType()!=accountType||!targetStock.getStockCode().equals(stockCode))throw new BusinessException(ErrorCode.INVALID_REQUEST);apply(x,b);return ok(row(regularBuys.save(x)),r);}
 @GetMapping("/cash-reserves") public ApiResponse<List<CashReserveRow>> cashReserves(HttpServletRequest r){return ok(cashReserves.findAll().stream().map(this::row).toList(),r);}
 @PostMapping("/cash-reserves") @Transactional public ApiResponse<CashReserveRow> createCashReserve(@Valid @RequestBody CashReserveRequest b,HttpServletRequest r){if(cashReserves.findByAccount_AccountId(b.accountId()).isPresent())throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);TbCashRsv x=new TbCashRsv();apply(x,b);return ok(row(cashReserves.save(x)),r);}
 @PutMapping("/cash-reserves/{id}") @Transactional public ApiResponse<CashReserveRow> updateCashReserve(@PathVariable Long id,@Valid @RequestBody CashReserveRequest b,HttpServletRequest r){TbCashRsv x=cashReserves.findById(id).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));cashReserves.findByAccount_AccountId(b.accountId()).filter(v->!v.getCashReserveId().equals(id)).ifPresent(v->{throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);});apply(x,b);return ok(row(cashReserves.save(x)),r);}
 private TbAcct account(Long id){return accounts.findById(id).filter(x->"N".equals(x.getDeleteYn())).orElseThrow(()->new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));} private TbStk stock(Long id){return stocks.findById(id).filter(x->"N".equals(x.getDeleteYn())).orElseThrow(()->new BusinessException(ErrorCode.STOCK_NOT_FOUND));}
 private void apply(TbHold x,HoldingRequest b){x.setAccount(account(b.accountId()));x.setStock(stock(b.stockId()));x.setHoldingQuantity(b.holdingQuantity());x.setAveragePrice(b.averagePrice());x.setExchangeRate(b.exchangeRate());x.setTargetWeight(b.targetWeight());x.setHoldingStatus(b.holdingStatus());x.setMemo(b.memo());x.setUseYn(nvl(b.useYn(),"Y"));x.setDeleteYn("N");}
 private void apply(TbRegBuy x,RegularBuyRequest b){TbAcct a=account(b.accountId());TbStk s=stock(b.stockId());x.setAccountType(a.getAccountType());x.setStockCode(s.getStockCode());x.setLegacyStockName(s.getStockName());x.setAccount(a);x.setStock(s);x.setBuyCycle(b.buyCycle());x.setBuyDayCode(b.buyCycle()==BuyCycle.WEEKLY?b.buyDayCode():null);x.setBuyDayNumber(null);x.setBuyDayNumbers(b.buyCycle()==BuyCycle.MONTHLY?b.buyDayNumbers():null);applyLegacySchedule(x,b);x.setBuyBasis(b.buyBasis());x.setMinimumBuyAmount(b.minimumBuyAmount());x.setMaximumBuyAmount(b.maximumBuyAmount());x.setBaseBuyQuantity(b.baseBuyQuantity());x.setBuyQuantity(b.buyQuantity());x.setMaximumMultiplier(b.maximumMultiplier());RegularBuyStatus status=b.buyStatus();String userPause=status==RegularBuyStatus.ACTIVE?nvl(b.userPauseYn(),"N"):"N";x.setBuyStatus(status);x.setPauseReason(b.pauseReason());x.setUserPauseYn(userPause);x.setAutoCalculateYn(nvl(b.autoCalculateYn(),"Y"));x.setLegacyActiveYn(status==RegularBuyStatus.ACTIVE?"Y":"N");x.setDeleteYn("N");}
 private void createDefaultRegularBuy(TbHold holding){TbRegBuy x=new TbRegBuy();x.setAccountType(holding.getAccount().getAccountType());x.setStockCode(holding.getStock().getStockCode());x.setLegacyStockName(holding.getStock().getStockName());x.setLegacyCycleType("MONTHLY");x.setLegacyMonthDay(15);x.setLegacyActiveYn("N");x.setAccount(holding.getAccount());x.setStock(holding.getStock());x.setBuyCycle(BuyCycle.MONTHLY);x.setBuyDayNumbers("15");x.setBuyBasis("AMOUNT");x.setMinimumBuyAmount(BigDecimal.ZERO);x.setMaximumMultiplier(new BigDecimal("3"));x.setBuyStatus(RegularBuyStatus.PAUSED);x.setPauseReason("기본 설정");x.setUserPauseYn("N");x.setAutoCalculateYn("Y");x.setRuleVersionNumber(1);x.setDeleteYn("N");regularBuys.save(x);}
 private void applyLegacySchedule(TbRegBuy x,RegularBuyRequest b){x.setLegacyWeekDays(null);x.setLegacyMonthDay(null);if(b.buyCycle()==BuyCycle.WEEKLY){x.setLegacyCycleType("WEEKLY");x.setLegacyWeekDays(b.buyDayCode());return;}if(b.buyCycle()==BuyCycle.MONTHLY&&b.buyDayNumbers()!=null&&!b.buyDayNumbers().contains(",")){x.setLegacyCycleType("MONTHLY");x.setLegacyMonthDay(Integer.valueOf(b.buyDayNumbers()));return;}x.setLegacyCycleType(b.buyCycle()==BuyCycle.DAILY?"DAILY":"MANUAL");}
 private void apply(TbCashRsv x,CashReserveRequest b){x.setAccount(account(b.accountId()));x.setReserveAmount(b.reserveAmount());x.setAccumulatedAmount(b.accumulatedAmount());x.setUsedAmount(b.usedAmount());x.setLastTransactionDate(b.lastTransactionDate());}
 private HoldingRow row(TbHold x){return new HoldingRow(x.getHoldingId(),x.getAccount().getAccountId(),x.getAccount().getAccountType(),x.getStock().getStockId(),x.getStock().getStockCode(),x.getStock().getStockName(),x.getHoldingQuantity(),x.getAveragePrice(),x.getCurrentPrice(),x.getExchangeRate(),x.getEvaluationAmount(),x.getProfitLossRate(),x.getTargetWeight(),x.getHoldingStatus(),x.getMemo(),x.getUseYn());}
 private RegularBuyRow row(TbRegBuy x){return new RegularBuyRow(x.getAccountType()+"/"+x.getStockCode(),x.getAccount().getAccountId(),x.getAccount().getAccountType(),x.getStock().getStockId(),x.getStock().getStockCode(),x.getStock().getStockName(),x.getBuyCycle(),x.getBuyDayCode(),x.getBuyDayNumber(),x.getBuyDayNumbers(),x.getBuyBasis(),x.getMinimumBuyAmount(),x.getMaximumBuyAmount(),x.getBaseBuyQuantity(),x.getBuyQuantity(),x.getMaximumMultiplier(),x.getRecommendedBuyAmount(),x.getBuyStatus(),x.getPauseReason(),x.getUserPauseYn(),x.getAutoCalculateYn());}
 private CashReserveRow row(TbCashRsv x){BigDecimal available=x.getAccumulatedAmount().subtract(x.getUsedAmount());return new CashReserveRow(x.getCashReserveId(),x.getAccount().getAccountId(),x.getAccount().getAccountType(),x.getReserveAmount(),x.getAccumulatedAmount(),x.getUsedAmount(),available,x.getLastTransactionDate(),x.getVersion());}
 private List<Map<String,Object>> regularBuyRows(){return jdbc.sql("""
  SELECT r."ACCT_TP"||'/'||r."STK_CD" AS "regularBuyKey",
   r."ACCT_ID" AS "accountId",r."ACCT_TP" AS "accountType",r."STK_ID" AS "stockId",
   a."BRKR_NM" AS "brokerName",s."MKT_CD" AS "marketCode",s."STK_CD" AS "stockCode",s."STK_NM" AS "stockName",
   r."PRIORITY" AS "priority",COALESCE(r."BASE_CYCLE_TP",r."BUY_CYCLE") AS "baseCycle",
   COALESCE(r."BASE_WEEK_DAY",r."BUY_DAY_CD") AS "baseWeekDays",r."BASE_MONTH_DAY" AS "baseMonthDay",
   r."BUY_BASIS" AS "buyBasis",r."MIN_BUY_AMT" AS "baseAmount",
   r."BASE_QTY" AS "baseQuantity",r."ACTV_YN" AS "activeYn",
   COALESCE(r."CYCLE_TP",r."BUY_CYCLE") AS "appliedCycle",COALESCE(r."WEEK_DAY",r."BUY_DAY_CD") AS "appliedWeekDays",
   r."MONTH_DAY" AS "appliedMonthDay",r."BUY_DAY_NOS" AS "appliedMonthDays",
   r."AMT" AS "appliedAmount",r."QTY" AS "appliedQuantity",r."RCMD_BUY_AMT" AS "recommendedAmount",
   r."PAUSE_RSN" AS "pauseReason",r."EXEC_ST" AS "todayBuyStatus",r."EXEC_NO" AS "executionNumber",
   COALESCE(ss."STK_GRD",CAST(s."STK_GRADE" AS VARCHAR)) AS "stockGrade",CAST(NULL AS VARCHAR) AS "investmentGrade",
   COALESCE(i."IDX_NM",ss."BM_CD") AS "benchmarkName",COALESCE(h."TGT_WGT",ss."TGT_WGT"*100) AS "targetWeight",
   ss."WGT_SCR" AS "weightScore",r."MEMO" AS "memo",
   r."BUY_CYCLE" AS "buyCycle",r."BUY_DAY_CD" AS "buyDayCode",r."BUY_DAY_NO" AS "buyDayNumber",r."BUY_DAY_NOS" AS "buyDayNumbers",
   r."MIN_BUY_AMT" AS "minimumBuyAmount",r."MAX_BUY_AMT" AS "maximumBuyAmount",r."BASE_QTY" AS "baseBuyQuantity",r."QTY" AS "buyQuantity",
   r."MAX_MULT" AS "maximumMultiplier",r."RCMD_BUY_AMT" AS "recommendedBuyAmount",r."BUY_STS" AS "buyStatus",
   r."USER_PAUSE_YN" AS "userPauseYn",r."AUTO_CALC_YN" AS "autoCalculateYn"
  FROM "TB_REG_BUY" r
  JOIN "TB_ACCT" a ON a."ACCT_ID"=r."ACCT_ID"
  JOIN "TB_STK" s ON s."STK_ID"=r."STK_ID"
  LEFT JOIN "TB_HOLD" h ON h."ACCT_ID"=r."ACCT_ID" AND h."STK_ID"=r."STK_ID" AND h."DEL_YN"='N'
  LEFT JOIN "TB_STK_SET" ss ON ss."ACCT_TP"=r."ACCT_TP" AND ss."STK_CD"=r."STK_CD"
  LEFT JOIN "TB_IDX" i ON i."IDX_ID"=s."BASE_IDX_ID"
  WHERE r."DEL_YN"='N'
  ORDER BY a."DISP_SEQ",s."STK_NM",r."STK_CD"
  """).query().listOfRows();} private String nvl(String v,String d){return v==null?d:v;} private <T> ApiResponse<T> ok(T d,HttpServletRequest r){return ApiResponse.success(d,TraceIdUtils.resolve(r));}
}

