package com.nanum.investment.common.api;

import com.nanum.investment.common.application.CommonCodeLookupService;
import com.nanum.investment.common.domain.AccountType;
import com.nanum.investment.common.domain.TbAcct;
import com.nanum.investment.common.domain.TbStk;
import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.common.exception.ErrorCode;
import com.nanum.investment.common.infrastructure.repository.TbAcctRepository;
import com.nanum.investment.common.infrastructure.repository.TbStkRepository;
import com.nanum.investment.common.response.ApiResponse;
import com.nanum.investment.common.web.TraceIdUtils;
import com.nanum.investment.holding.application.HoldingValuationService;
import com.nanum.investment.holding.application.PortfolioWeightRefreshService;
import com.nanum.investment.holding.domain.HoldingStatus;
import com.nanum.investment.holding.domain.TbCashRsv;
import com.nanum.investment.holding.domain.TbHold;
import com.nanum.investment.holding.domain.WeightStatus;
import com.nanum.investment.holding.infrastructure.repository.TbCashRsvRepository;
import com.nanum.investment.holding.infrastructure.repository.TbHoldRepository;
import com.nanum.investment.regularbuy.domain.BuyCycle;
import com.nanum.investment.regularbuy.domain.RegularBuyStatus;
import com.nanum.investment.regularbuy.domain.TbRegBuy;
import com.nanum.investment.regularbuy.domain.TbRegBuyId;
import com.nanum.investment.regularbuy.infrastructure.repository.TbRegBuyRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/operations")
@Transactional(readOnly = true)
public class OperationalDataAdminController {
  private final TbHoldRepository holdings;
  private final TbRegBuyRepository regularBuys;
  private final TbCashRsvRepository cashReserves;
  private final TbAcctRepository accounts;
  private final TbStkRepository stocks;
  private final JdbcClient jdbc;
  private final CommonCodeLookupService commonCodes;
  private final HoldingValuationService holdingValuations;
  private final PortfolioWeightRefreshService portfolioWeights;

  public OperationalDataAdminController(
      TbHoldRepository holdings,
      TbRegBuyRepository regularBuys,
      TbCashRsvRepository cashReserves,
      TbAcctRepository accounts,
      TbStkRepository stocks,
      JdbcClient jdbc,
      CommonCodeLookupService commonCodes,
      HoldingValuationService holdingValuations,
      PortfolioWeightRefreshService portfolioWeights) {
    this.holdings = holdings;
    this.regularBuys = regularBuys;
    this.cashReserves = cashReserves;
    this.accounts = accounts;
    this.stocks = stocks;
    this.jdbc = jdbc;
    this.commonCodes = commonCodes;
    this.holdingValuations = holdingValuations;
    this.portfolioWeights = portfolioWeights;
  }

  public record HoldingRequest(
      @NotNull Long accountId,
      @NotNull Long stockId,
      @NotNull @DecimalMin("0") BigDecimal holdingQuantity,
      @NotNull @DecimalMin("0") BigDecimal averagePrice,
      @DecimalMin("0") BigDecimal wholeSharePurchaseAmount,
      @DecimalMin("0") BigDecimal fractionalSharePurchaseAmount,
      @NotNull @DecimalMin("0") BigDecimal exchangeRate,
      @DecimalMin("0") @DecimalMax("100") BigDecimal targetWeight,
      @NotNull HoldingStatus holdingStatus,
      @Size(max = 1000) String memo,
      @Pattern(regexp = "[YN]") String useYn) {}

  public record HoldingRow(
      Long holdingId,
      Long accountId,
      AccountType accountType,
      Long stockId,
      String stockCode,
      String stockName,
      BigDecimal holdingQuantity,
      BigDecimal averagePrice,
      BigDecimal wholeSharePurchaseAmount,
      BigDecimal fractionalSharePurchaseAmount,
      BigDecimal currentPrice,
      BigDecimal exchangeRate,
      BigDecimal evaluationAmount,
      BigDecimal profitLossRate,
      BigDecimal targetWeight,
      BigDecimal currentWeight,
      WeightStatus weightStatus,
      String weightStatusName,
      HoldingStatus holdingStatus,
      RegularBuyStatus buyStatus,
      String userPauseYn,
      String memo,
      String useYn) {}

  public record RegularBuyRequest(
      @NotNull Long accountId,
      @NotNull Long stockId,
      @Min(1) Integer priority,
      @Size(max = 50) String investmentGrade,
      @Size(max = 1000) String memo,
      @NotNull BuyCycle buyCycle,
      @Pattern(regexp = "(MON|TUE|WED|THU|FRI)(,(MON|TUE|WED|THU|FRI))*") String buyDayCode,
      @Min(1) @Max(31) Integer buyDayNumber,
      @Pattern(regexp = "([1-9]|[12][0-9]|3[01])(,([1-9]|[12][0-9]|3[01]))*") String buyDayNumbers,
      @NotNull @Pattern(regexp = "DAILY|WEEKLY|MONTHLY") String appliedCycle,
      @Pattern(regexp = "(MON|TUE|WED|THU|FRI)(,(MON|TUE|WED|THU|FRI))*") String appliedWeekDays,
      @Pattern(regexp = "([1-9]|[12][0-9]|3[01])(,([1-9]|[12][0-9]|3[01]))*")
          String appliedMonthDays,
      @DecimalMin("0") BigDecimal appliedAmount,
      @NotNull @Pattern(regexp = "AMOUNT|QUANTITY") String buyBasis,
      @NotNull @DecimalMin("0") BigDecimal minimumBuyAmount,
      @DecimalMin("0") BigDecimal baseBuyQuantity,
      @DecimalMin("0") BigDecimal buyQuantity,
      @NotNull RegularBuyStatus buyStatus,
      @Size(max = 500) String pauseReason,
      @Pattern(regexp = "[YN]") String userPauseYn,
      @Pattern(regexp = "[YN]") String autoCalculateYn) {}

  public record RegularBuyRow(
      String regularBuyKey,
      Long accountId,
      AccountType accountType,
      Long stockId,
      String stockCode,
      String stockName,
      BuyCycle buyCycle,
      String buyDayCode,
      Integer buyDayNumber,
      String buyDayNumbers,
      String buyBasis,
      BigDecimal minimumBuyAmount,
      BigDecimal baseBuyQuantity,
      BigDecimal buyQuantity,
      BigDecimal recommendedBuyAmount,
      RegularBuyStatus buyStatus,
      String pauseReason,
      String userPauseYn,
      String autoCalculateYn) {}

  public record CashReserveRequest(
      @NotNull Long accountId,
      @NotNull @DecimalMin("0") BigDecimal reserveAmount,
      @NotNull @DecimalMin("0") BigDecimal accumulatedAmount,
      @NotNull @DecimalMin("0") BigDecimal usedAmount,
      LocalDate lastTransactionDate) {}

  public record CashReserveRow(
      Long cashReserveId,
      Long accountId,
      AccountType accountType,
      BigDecimal reserveAmount,
      BigDecimal accumulatedAmount,
      BigDecimal usedAmount,
      BigDecimal availableAmount,
      LocalDate lastTransactionDate,
      Long version) {}

  @GetMapping("/holdings")
  public ApiResponse<List<HoldingRow>> holdings(HttpServletRequest r) {
    List<TbHold> rows =
        holdings.findAll().stream().filter(x -> "N".equals(x.getDeleteYn())).toList();
    Map<Long, BigDecimal> totals =
        rows.stream()
            .filter(x -> "Y".equals(x.getUseYn()))
            .collect(
                Collectors.groupingBy(
                    x -> x.getAccount().getAccountId(),
                    Collectors.reducing(
                        BigDecimal.ZERO, x -> nvl(x.getEvaluationAmount()), BigDecimal::add)));
    Map<String, TbRegBuy> buySettings =
        regularBuys.findAll().stream()
            .filter(x -> "N".equals(x.getDeleteYn()))
            .collect(
                Collectors.toMap(
                    x -> holdingKey(x.getAccount().getAccountId(), x.getStock().getStockId()),
                    x -> x,
                    (a, b) -> a));
    Map<String, String> weightStatusNames = commonCodes.activeNames("WGT_STS");
    return ok(
        rows.stream()
            .map(
                x ->
                    row(
                        x,
                        totals.getOrDefault(x.getAccount().getAccountId(), BigDecimal.ZERO),
                        buySettings.get(
                            holdingKey(x.getAccount().getAccountId(), x.getStock().getStockId())),
                        weightStatusNames))
            .toList(),
        r);
  }

  @PostMapping("/holdings")
  @Transactional
  public ApiResponse<HoldingRow> createHolding(
      @Valid @RequestBody HoldingRequest b, HttpServletRequest r) {
    if (holdings
        .findByAccount_AccountIdAndStock_StockIdAndDeleteYn(b.accountId(), b.stockId(), "N")
        .isPresent()) throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
    TbHold x = new TbHold();
    apply(x, b);
    TbHold saved = holdings.save(x);
    createDefaultRegularBuy(saved);
    portfolioWeights.refreshAccount(saved.getAccount());
    return ok(row(saved), r);
  }

  @PutMapping("/holdings/{id}")
  @Transactional
  public ApiResponse<HoldingRow> updateHolding(
      @PathVariable Long id, @Valid @RequestBody HoldingRequest b, HttpServletRequest r) {
    TbHold x =
        holdings
            .findById(id)
            .filter(v -> "N".equals(v.getDeleteYn()))
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    holdings
        .findByAccount_AccountIdAndStock_StockIdAndDeleteYn(b.accountId(), b.stockId(), "N")
        .filter(v -> !v.getHoldingId().equals(id))
        .ifPresent(
            v -> {
              throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
            });
    apply(x, b);
    TbHold saved = holdings.save(x);
    portfolioWeights.refreshAccount(saved.getAccount());
    return ok(row(saved), r);
  }

  @GetMapping("/investment-grades")
  public ApiResponse<List<Map<String, Object>>> investmentGrades(HttpServletRequest r) {
    return ok(
        jdbc.sql(
                """
  SELECT "CD_NM" AS "investmentGrade",CAST("CD_KEY" AS INTEGER) AS "weightScore","DESC" AS "description"
  FROM "TB_CD_DTL"
  WHERE "CD_GRP"='INVESTMENT_GRADE' AND "ACTV_YN"='Y'
  ORDER BY "DSP_ORD"
  """)
            .query()
            .listOfRows(),
        r);
  }

  @GetMapping("/regular-buys")
  public ApiResponse<List<Map<String, Object>>> regularBuys(HttpServletRequest r) {
    return ok(regularBuyRows(), r);
  }

  @PostMapping("/regular-buys")
  @Transactional
  public ApiResponse<RegularBuyRow> createRegularBuy(
      @Valid @RequestBody RegularBuyRequest b, HttpServletRequest r) {
    if (regularBuys
        .findByAccount_AccountIdAndStock_StockIdAndDeleteYn(b.accountId(), b.stockId(), "N")
        .isPresent()) throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
    TbRegBuy x = new TbRegBuy();
    apply(x, b);
    return ok(row(regularBuys.save(x)), r);
  }

  @PutMapping("/regular-buys/{accountType}/{stockCode}")
  @Transactional
  public ApiResponse<RegularBuyRow> updateRegularBuy(
      @PathVariable AccountType accountType,
      @PathVariable String stockCode,
      @Valid @RequestBody RegularBuyRequest b,
      HttpServletRequest r) {
    TbRegBuy x =
        regularBuys
            .findById(new TbRegBuyId(accountType, stockCode))
            .filter(v -> "N".equals(v.getDeleteYn()))
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    TbAcct targetAccount = account(b.accountId());
    TbStk targetStock = stock(b.stockId());
    if (targetAccount.getAccountType() != accountType
        || !targetStock.getStockCode().equals(stockCode))
      throw new BusinessException(ErrorCode.INVALID_REQUEST);
    apply(x, b);
    return ok(row(regularBuys.save(x)), r);
  }

  @GetMapping("/cash-reserves")
  public ApiResponse<List<CashReserveRow>> cashReserves(HttpServletRequest r) {
    return ok(cashReserves.findAll().stream().map(this::row).toList(), r);
  }

  @PostMapping("/cash-reserves")
  @Transactional
  public ApiResponse<CashReserveRow> createCashReserve(
      @Valid @RequestBody CashReserveRequest b, HttpServletRequest r) {
    if (cashReserves.findByAccount_AccountId(b.accountId()).isPresent())
      throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
    TbCashRsv x = new TbCashRsv();
    apply(x, b);
    return ok(row(cashReserves.save(x)), r);
  }

  @PutMapping("/cash-reserves/{id}")
  @Transactional
  public ApiResponse<CashReserveRow> updateCashReserve(
      @PathVariable Long id, @Valid @RequestBody CashReserveRequest b, HttpServletRequest r) {
    TbCashRsv x =
        cashReserves
            .findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    cashReserves
        .findByAccount_AccountId(b.accountId())
        .filter(v -> !v.getCashReserveId().equals(id))
        .ifPresent(
            v -> {
              throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
            });
    apply(x, b);
    return ok(row(cashReserves.save(x)), r);
  }

  private TbAcct account(Long id) {
    return accounts
        .findById(id)
        .filter(x -> "N".equals(x.getDeleteYn()))
        .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
  }

  private TbStk stock(Long id) {
    return stocks
        .findById(id)
        .filter(x -> "N".equals(x.getDeleteYn()))
        .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
  }

  private void apply(TbHold x, HoldingRequest b) {
    TbAcct account = account(b.accountId());
    boolean domestic = account.getAccountType() == AccountType.DOMESTIC;
    if (domestic
        && (b.wholeSharePurchaseAmount() == null || b.fractionalSharePurchaseAmount() == null))
      throw new BusinessException(
          ErrorCode.INVALID_REQUEST, "국내주식 계좌는 정수주·소수점주 매입금액을 모두 입력해야 합니다.");
    if (!domestic
        && (b.wholeSharePurchaseAmount() != null || b.fractionalSharePurchaseAmount() != null))
      throw new BusinessException(
          ErrorCode.INVALID_REQUEST, "정수주·소수점주 매입금액은 국내주식 계좌에서만 입력할 수 있습니다.");
    BigDecimal averagePrice = b.averagePrice();
    if (domestic) {
      BigDecimal totalPurchaseAmount =
          b.wholeSharePurchaseAmount().add(b.fractionalSharePurchaseAmount());
      if (b.holdingQuantity().signum() == 0 && totalPurchaseAmount.signum() != 0)
        throw new BusinessException(
            ErrorCode.INVALID_REQUEST, "보유수량이 0이면 정수주·소수점주 매입금액도 0이어야 합니다.");
      averagePrice =
          b.holdingQuantity().signum() == 0
              ? BigDecimal.ZERO.setScale(6)
              : totalPurchaseAmount.divide(b.holdingQuantity(), 6, RoundingMode.HALF_UP);
    }
    x.setAccount(account);
    x.setStock(stock(b.stockId()));
    x.setHoldingQuantity(b.holdingQuantity());
    x.setAveragePrice(averagePrice);
    x.setWholeSharePurchaseAmount(b.wholeSharePurchaseAmount());
    x.setFractionalSharePurchaseAmount(b.fractionalSharePurchaseAmount());
    x.setExchangeRate(b.exchangeRate());
    BigDecimal currentPrice = x.getCurrentPrice() == null ? BigDecimal.ZERO : x.getCurrentPrice();
    HoldingValuationService.Valuation valuation =
        holdingValuations.calculate(
            b.holdingQuantity(), averagePrice, currentPrice, b.exchangeRate());
    x.setOriginalEvaluationAmount(valuation.originalEvaluationAmount());
    x.setEvaluationAmount(valuation.evaluationAmount());
    x.setOriginalProfitLossAmount(valuation.originalProfitLossAmount());
    x.setProfitLossAmount(valuation.profitLossAmount());
    x.setProfitLossRate(valuation.profitLossRate());
    x.setCalculatedDateTime(OffsetDateTime.now());
    x.setTargetWeight(b.targetWeight());
    x.setHoldingStatus(b.holdingStatus());
    x.setMemo(b.memo());
    x.setUseYn(nvl(b.useYn(), "Y"));
    x.setDeleteYn("N");
  }

  private void apply(TbRegBuy x, RegularBuyRequest b) {
    TbAcct a = account(b.accountId());
    TbStk s = stock(b.stockId());
    AccountType accountType = a.getAccountType();
    x.setAccountType(accountType);
    x.setStockCode(s.getStockCode());
    x.setAccount(a);
    x.setStock(s);
    x.setPriority(b.priority());
    x.setInvestmentGrade(investmentGrade(b.investmentGrade()));
    x.setMemo(b.memo());
    x.setBuyCycle(BuyCycle.WEEKLY);
    x.setBuyDayCode("TUE");
    x.setBuyDayNumber(null);
    x.setBuyDayNumbers(null);
    applyAppliedSchedule(x, b);
    x.setAppliedAmount(b.appliedAmount());
    x.setBuyBasis(b.buyBasis());
    x.setMinimumBuyAmount(new BigDecimal("10000"));
    x.setBaseBuyQuantity(BigDecimal.ONE);
    x.setBuyQuantity(b.buyQuantity());
    RegularBuyStatus status = b.buyStatus();
    String userPause = status == RegularBuyStatus.ACTIVE ? nvl(b.userPauseYn(), "N") : "N";
    x.setBuyStatus(status);
    x.setPauseReason(b.pauseReason());
    x.setUserPauseYn(userPause);
    x.setAutoCalculateYn(nvl(b.autoCalculateYn(), "Y"));
    x.setLegacyActiveYn(status == RegularBuyStatus.ACTIVE ? "Y" : "N");
    x.setDeleteYn("N");
  }

  private String investmentGrade(String value) {
    if (value == null || value.isBlank()) return null;
    String grade = value.trim();
    Long count =
        jdbc.sql(
                "SELECT COUNT(*) FROM \"TB_CD_DTL\" WHERE \"CD_GRP\"='INVESTMENT_GRADE' AND \"CD_NM\"=:grade AND \"ACTV_YN\"='Y'")
            .param("grade", grade)
            .query(Long.class)
            .single();
    if (count == 0) throw new BusinessException(ErrorCode.INVALID_REQUEST);
    return grade;
  }

  private void createDefaultRegularBuy(TbHold holding) {
    AccountType accountType = holding.getAccount().getAccountType();
    boolean quantityDefault = accountType == AccountType.ISA || accountType == AccountType.PENSION;
    TbRegBuy x = new TbRegBuy();
    x.setAccountType(accountType);
    x.setStockCode(holding.getStock().getStockCode());
    x.setLegacyCycleType("MONTHLY");
    x.setLegacyMonthDay(15);
    x.setAppliedMonthDays("15");
    x.setLegacyActiveYn("N");
    x.setAccount(holding.getAccount());
    x.setStock(holding.getStock());
    x.setBuyCycle(BuyCycle.WEEKLY);
    x.setBuyDayCode("TUE");
    x.setBuyDayNumbers(null);
    x.setBuyBasis(quantityDefault ? "QUANTITY" : "AMOUNT");
    x.setMinimumBuyAmount(new BigDecimal("10000"));
    x.setBaseBuyQuantity(BigDecimal.ONE);
    x.setBuyStatus(RegularBuyStatus.PAUSED);
    x.setPauseReason("기본 설정");
    x.setUserPauseYn("N");
    x.setAutoCalculateYn("Y");
    x.setRuleVersionNumber(1);
    x.setDeleteYn("N");
    regularBuys.save(x);
  }

  private void applyAppliedSchedule(TbRegBuy x, RegularBuyRequest b) {
    x.setLegacyCycleType(b.appliedCycle());
    x.setLegacyWeekDays(b.appliedCycle().equals("WEEKLY") ? b.appliedWeekDays() : null);
    String monthDays = b.appliedCycle().equals("MONTHLY") ? b.appliedMonthDays() : null;
    x.setAppliedMonthDays(monthDays);
    x.setLegacyMonthDay(monthDays == null ? null : Integer.valueOf(monthDays.split(",")[0]));
  }

  private void apply(TbCashRsv x, CashReserveRequest b) {
    x.setAccount(account(b.accountId()));
    x.setReserveAmount(b.reserveAmount());
    x.setAccumulatedAmount(b.accumulatedAmount());
    x.setUsedAmount(b.usedAmount());
    x.setLastTransactionDate(b.lastTransactionDate());
  }

  private HoldingRow row(TbHold x) {
    TbRegBuy buy =
        regularBuys
            .findByAccount_AccountIdAndStock_StockIdAndDeleteYn(
                x.getAccount().getAccountId(), x.getStock().getStockId(), "N")
            .orElse(null);
    return row(
        x,
        accountEvaluationTotal(x.getAccount().getAccountId()),
        buy,
        commonCodes.activeNames("WGT_STS"));
  }

  private HoldingRow row(
      TbHold x, BigDecimal accountTotal, TbRegBuy buy, Map<String, String> weightStatusNames) {
    BigDecimal currentWeight = x.getCurrentWeight();
    if (currentWeight == null && accountTotal.signum() > 0)
      currentWeight =
          nvl(x.getEvaluationAmount())
              .multiply(new BigDecimal("100"))
              .divide(accountTotal, 4, RoundingMode.HALF_UP);
    return new HoldingRow(
        x.getHoldingId(),
        x.getAccount().getAccountId(),
        x.getAccount().getAccountType(),
        x.getStock().getStockId(),
        x.getStock().getStockCode(),
        x.getStock().getStockName(),
        x.getHoldingQuantity(),
        x.getAveragePrice(),
        x.getWholeSharePurchaseAmount(),
        x.getFractionalSharePurchaseAmount(),
        x.getCurrentPrice(),
        x.getExchangeRate(),
        x.getEvaluationAmount(),
        x.getProfitLossRate(),
        x.getTargetWeight(),
        currentWeight,
        x.getWeightStatus(),
        x.getWeightStatus() == null ? null : weightStatusNames.get(x.getWeightStatus().name()),
        x.getHoldingStatus(),
        buy == null ? null : buy.getBuyStatus(),
        buy == null ? "N" : buy.getUserPauseYn(),
        x.getMemo(),
        x.getUseYn());
  }

  private String holdingKey(Long accountId, Long stockId) {
    return accountId + ":" + stockId;
  }

  private BigDecimal accountEvaluationTotal(Long accountId) {
    return jdbc.sql(
            "SELECT COALESCE(SUM(COALESCE(\"EVL_AMT\",0)),0) FROM \"TB_HOLD\" WHERE \"ACCT_ID\"=:accountId AND \"DEL_YN\"='N' AND \"USE_YN\"='Y'")
        .param("accountId", accountId)
        .query(BigDecimal.class)
        .single();
  }

  private RegularBuyRow row(TbRegBuy x) {
    return new RegularBuyRow(
        x.getAccountType() + "/" + x.getStockCode(),
        x.getAccount().getAccountId(),
        x.getAccount().getAccountType(),
        x.getStock().getStockId(),
        x.getStock().getStockCode(),
        x.getStock().getStockName(),
        x.getBuyCycle(),
        x.getBuyDayCode(),
        x.getBuyDayNumber(),
        x.getBuyDayNumbers(),
        x.getBuyBasis(),
        x.getMinimumBuyAmount(),
        x.getBaseBuyQuantity(),
        x.getBuyQuantity(),
        x.getRecommendedBuyAmount(),
        x.getBuyStatus(),
        x.getPauseReason(),
        x.getUserPauseYn(),
        x.getAutoCalculateYn());
  }

  private CashReserveRow row(TbCashRsv x) {
    BigDecimal available = x.getAccumulatedAmount().subtract(x.getUsedAmount());
    return new CashReserveRow(
        x.getCashReserveId(),
        x.getAccount().getAccountId(),
        x.getAccount().getAccountType(),
        x.getReserveAmount(),
        x.getAccumulatedAmount(),
        x.getUsedAmount(),
        available,
        x.getLastTransactionDate(),
        x.getVersion());
  }

  private List<Map<String, Object>> regularBuyRows() {
    return jdbc.sql(
            """
  SELECT r."ACCT_TP"||'/'||r."STK_CD" AS "regularBuyKey",
   r."ACCT_ID" AS "accountId",r."ACCT_TP" AS "accountType",r."STK_ID" AS "stockId",
   s."MKT_CD" AS "marketCode",s."STK_CD" AS "stockCode",s."STK_NM" AS "stockName",
   r."PRIORITY" AS "priority",r."BUY_CYCLE" AS "baseCycle",
   r."BUY_DAY_CD" AS "baseWeekDays",r."BUY_DAY_NOS" AS "baseMonthDays",
   r."BUY_BASIS" AS "buyBasis",r."MIN_BUY_AMT" AS "baseAmount",
   r."BASE_QTY" AS "baseQuantity",r."ACTV_YN" AS "activeYn",
   COALESCE(r."CYCLE_TP",r."BUY_CYCLE") AS "appliedCycle",COALESCE(r."WEEK_DAY",r."BUY_DAY_CD") AS "appliedWeekDays",
   r."APPLIED_DAY_NOS" AS "appliedMonthDays",
   r."AMT" AS "appliedAmount",r."QTY" AS "appliedQuantity",r."RCMD_BUY_AMT" AS "recommendedAmount",
   r."PAUSE_RSN" AS "pauseReason",r."EXEC_ST" AS "todayBuyStatus",r."EXEC_NO" AS "executionNumber",
   COALESCE(ss."STK_GRD",CAST(s."STK_GRADE" AS VARCHAR)) AS "stockGrade",r."INV_GRD" AS "investmentGrade",
   COALESCE(i."IDX_NM",ss."BM_CD") AS "benchmarkName",COALESCE(h."TGT_WGT",ss."TGT_WGT"*100) AS "targetWeight",
   COALESCE(h."CUR_WGT",
     CASE WHEN hw."TOTAL_EVALUATION" > 0
       THEN h."EVL_AMT" * 100 / hw."TOTAL_EVALUATION"
     END) AS "currentWeight",
   COALESCE(CAST(ig."CD_KEY" AS INTEGER),ss."WGT_SCR") AS "weightScore",r."MEMO" AS "memo",
   r."BUY_CYCLE" AS "buyCycle",r."BUY_DAY_CD" AS "buyDayCode",r."BUY_DAY_NO" AS "buyDayNumber",r."BUY_DAY_NOS" AS "buyDayNumbers",
   r."MIN_BUY_AMT" AS "minimumBuyAmount",r."BASE_QTY" AS "baseBuyQuantity",r."QTY" AS "buyQuantity",
   r."RCMD_BUY_AMT" AS "recommendedBuyAmount",r."BUY_STS" AS "buyStatus",
   r."USER_PAUSE_YN" AS "userPauseYn",r."AUTO_CALC_YN" AS "autoCalculateYn"
  FROM "TB_REG_BUY" r
  JOIN "TB_ACCT" a ON a."ACCT_ID"=r."ACCT_ID"
  JOIN "TB_STK" s ON s."STK_ID"=r."STK_ID"
  LEFT JOIN "TB_HOLD" h ON h."ACCT_ID"=r."ACCT_ID" AND h."STK_ID"=r."STK_ID" AND h."DEL_YN"='N'
  LEFT JOIN (
    SELECT "ACCT_ID",SUM(COALESCE("EVL_AMT",0)) AS "TOTAL_EVALUATION"
    FROM "TB_HOLD"
    WHERE "DEL_YN"='N' AND "USE_YN"='Y'
    GROUP BY "ACCT_ID"
  ) hw ON hw."ACCT_ID"=r."ACCT_ID"
  LEFT JOIN "TB_STK_SET" ss ON ss."ACCT_TP"=r."ACCT_TP" AND ss."STK_CD"=r."STK_CD"
  LEFT JOIN "TB_IDX" i ON i."IDX_ID"=s."BASE_IDX_ID"
  LEFT JOIN "TB_CD_DTL" ig ON ig."CD_GRP"='INVESTMENT_GRADE' AND ig."CD_NM"=r."INV_GRD" AND ig."ACTV_YN"='Y'
  WHERE r."DEL_YN"='N'
  ORDER BY a."DISP_SEQ",s."STK_NM",r."STK_CD"
  """)
        .query()
        .listOfRows();
  }

  private BigDecimal nvl(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  private String nvl(String v, String d) {
    return v == null ? d : v;
  }

  private <T> ApiResponse<T> ok(T d, HttpServletRequest r) {
    return ApiResponse.success(d, TraceIdUtils.resolve(r));
  }
}
