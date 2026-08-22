package com.nanum.investment.rebalance.application;

import com.nanum.investment.rebalance.domain.RebalanceType;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutomaticRebalanceService {
  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private final JdbcClient jdbc;
  private final RebalanceCalculationService calculator;

  public AutomaticRebalanceService(JdbcClient jdbc, RebalanceCalculationService calculator) {
    this.jdbc = jdbc;
    this.calculator = calculator;
  }

  @Transactional
  public List<AutomaticRebalanceResult> generate(LocalDate date) {
    return List.of(generate(date, RebalanceType.WEEKLY), generate(date, RebalanceType.MONTHLY));
  }

  @Transactional
  public AutomaticRebalanceResult generate(LocalDate date, RebalanceType type) {
    if (date == null) throw new IllegalArgumentException("리밸런싱 기준일이 필요합니다.");
    Long decision = latestDecision(date);
    List<AutomaticRebalanceResult.AccountPlan> accounts = new ArrayList<>();
    List<AutomaticRebalanceResult.Item> items = new ArrayList<>();
    for (Account account : accounts()) {
      List<Position> positions = positions(account.id(), decision);
      BigDecimal holdings =
          positions.stream().map(Position::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
      var amounts =
          calculator.account(
              account.cash(),
              account.reserve(),
              holdings,
              account.targetCashWeight(),
              BigDecimal.ZERO);
      jdbc.sql(
              "UPDATE \"TB_REBAL\" SET \"LATEST_YN\"='N' WHERE \"ACCT_ID\"=:account AND \"REBAL_TP\"=:type AND \"LATEST_YN\"='Y'")
          .param("account", account.id())
          .param("type", type.name())
          .update();
      Integer sequence =
          jdbc.sql(
                  "SELECT COALESCE(max(\"CALC_SEQ\"),0)+1 FROM \"TB_REBAL\" WHERE \"BASE_DT\"=:day AND \"ACCT_ID\"=:account AND \"REBAL_TP\"=:type")
              .param("day", date)
              .param("account", account.id())
              .param("type", type.name())
              .query(Integer.class)
              .single();
      Long rebalanceId = insertParent(date, type, decision, account, amounts, sequence);
      BigDecimal buyRemaining = amounts.buyBudgetAmount(),
          sellRemaining = amounts.cashGapAmount().negate().max(BigDecimal.ZERO),
          buyTotal = BigDecimal.ZERO,
          sellTotal = BigDecimal.ZERO;
      int priority = 1, buyCount = 0, sellCount = 0, holdCount = 0;
      for (Position p : positions) {
        BigDecimal targetWeight = percent(p.targetWeight());
        var calculated =
            calculator.item(
                amounts.totalAssetAmount(),
                p.amount(),
                targetWeight,
                buyRemaining,
                p.weightLimit(amounts.totalAssetAmount()),
                sellRemaining,
                p.fundamentalDamaged(),
                false);
        BigDecimal currentRegular = p.currentRegular(), newRegular = p.recommendedRegular();
        String action =
            type == RebalanceType.WEEKLY
                ? regularAction(currentRegular, newRegular)
                : calculated.action().name();
        BigDecimal change =
            type == RebalanceType.WEEKLY
                ? newRegular.subtract(currentRegular)
                : calculated.recommendedBuyAmount().subtract(calculated.recommendedSellAmount());
        BigDecimal
            buy =
                type == RebalanceType.MONTHLY ? calculated.recommendedBuyAmount() : BigDecimal.ZERO,
            sell =
                type == RebalanceType.MONTHLY
                    ? calculated.recommendedSellAmount()
                    : BigDecimal.ZERO;
        buyRemaining = buyRemaining.subtract(buy).max(BigDecimal.ZERO);
        sellRemaining = sellRemaining.subtract(sell).max(BigDecimal.ZERO);
        buyTotal = buyTotal.add(buy);
        sellTotal = sellTotal.add(sell);
        if ("BUY".equals(action) || "INCREASE_REGULAR_BUY".equals(action)) buyCount++;
        else if ("SELL".equals(action)
            || "REDUCE_REGULAR_BUY".equals(action)
            || "PAUSE_REGULAR_BUY".equals(action)) sellCount++;
        else holdCount++;
        String reason = type == RebalanceType.WEEKLY ? "8단계 추천 정기매수액 반영" : "현재 평가금액과 목표비중 차이 반영";
        Long itemId =
            insertItem(
                rebalanceId,
                date,
                account,
                p,
                calculated,
                currentRegular,
                newRegular,
                change,
                action,
                priority,
                reason,
                buy,
                sell);
        items.add(
            new AutomaticRebalanceResult.Item(
                itemId,
                rebalanceId,
                account.type(),
                p.code(),
                p.name(),
                p.amount(),
                calculated.targetAmount(),
                p.currentWeight(),
                targetWeight,
                currentRegular,
                newRegular,
                change,
                action,
                priority,
                reason));
        priority++;
      }
      boolean required =
          items.stream()
              .filter(x -> x.rebalanceId().equals(rebalanceId))
              .anyMatch(x -> !"HOLD".equals(x.action()));
      jdbc.sql(
              "UPDATE \"TB_REBAL\" SET \"RCMD_BUY_TOT_AMT\"=:buy,\"RCMD_SELL_TOT_AMT\"=:sell,\"ITEM_CNT\"=:items,\"BUY_ITEM_CNT\"=:buys,\"SELL_ITEM_CNT\"=:sells,\"HOLD_ITEM_CNT\"=:holds,\"REBAL_REQ_YN\"=:required WHERE \"REBAL_ID\"=:id")
          .param("buy", buyTotal)
          .param("sell", sellTotal)
          .param("items", positions.size())
          .param("buys", buyCount)
          .param("sells", sellCount)
          .param("holds", holdCount)
          .param("required", required ? "Y" : "N")
          .param("id", rebalanceId)
          .update();
      accounts.add(
          new AutomaticRebalanceResult.AccountPlan(
              rebalanceId,
              account.id(),
              account.type(),
              amounts.totalAssetAmount(),
              buyTotal,
              sellTotal,
              positions.size(),
              required));
    }
    return new AutomaticRebalanceResult(
        date, type.name(), List.copyOf(accounts), List.copyOf(items));
  }

  public AutomaticRebalanceResult latest(RebalanceType type) {
    LocalDate date =
        jdbc.sql("SELECT max(\"BASE_DT\") FROM \"TB_REBAL\" WHERE \"REBAL_TP\"=:type")
            .param("type", type.name())
            .query(LocalDate.class)
            .optional()
            .orElse(null);
    if (date == null) return new AutomaticRebalanceResult(null, type.name(), List.of(), List.of());
    return view(date, type);
  }

  private AutomaticRebalanceResult view(LocalDate date, RebalanceType type) {
    List<AutomaticRebalanceResult.AccountPlan> accounts =
        jdbc.sql(
                "SELECT r.\"REBAL_ID\",r.\"ACCT_ID\",a.\"ACCT_TP\",r.\"TOT_AST_AMT\",r.\"RCMD_BUY_TOT_AMT\",r.\"RCMD_SELL_TOT_AMT\",r.\"ITEM_CNT\",r.\"REBAL_REQ_YN\" FROM \"TB_REBAL\" r JOIN \"TB_ACCT\" a ON a.\"ACCT_ID\"=r.\"ACCT_ID\" WHERE r.\"BASE_DT\"=:day AND r.\"REBAL_TP\"=:type AND r.\"LATEST_YN\"='Y' ORDER BY a.\"DISP_SEQ\"")
            .param("day", date)
            .param("type", type.name())
            .query(
                (rs, n) ->
                    new AutomaticRebalanceResult.AccountPlan(
                        rs.getLong(1),
                        rs.getLong(2),
                        rs.getString(3),
                        rs.getBigDecimal(4),
                        rs.getBigDecimal(5),
                        rs.getBigDecimal(6),
                        rs.getInt(7),
                        "Y".equals(rs.getString(8))))
            .list();
    List<AutomaticRebalanceResult.Item> items =
        jdbc.sql(
                "SELECT i.\"REBAL_ITEM_ID\",i.\"REBAL_ID\",a.\"ACCT_TP\",s.\"STK_CD\",s.\"STK_NM\",i.\"CUR_AMT\",i.\"TGT_AMT\",i.\"CUR_WGT\",i.\"TGT_WGT\",i.\"CUR_REG_BUY_AMT\",i.\"NEW_REG_BUY_AMT\",i.\"REG_BUY_CHG_AMT\",i.\"REBAL_ACT\",i.\"PRIO_NO\",i.\"DEC_RSN\" FROM \"TB_REBAL_ITEM\" i JOIN \"TB_REBAL\" r ON r.\"REBAL_ID\"=i.\"REBAL_ID\" JOIN \"TB_ACCT\" a ON a.\"ACCT_ID\"=i.\"ACCT_ID\" JOIN \"TB_STK\" s ON s.\"STK_ID\"=i.\"STK_ID\" WHERE r.\"BASE_DT\"=:day AND r.\"REBAL_TP\"=:type AND r.\"LATEST_YN\"='Y' ORDER BY a.\"DISP_SEQ\",i.\"PRIO_NO\"")
            .param("day", date)
            .param("type", type.name())
            .query(
                (rs, n) ->
                    new AutomaticRebalanceResult.Item(
                        rs.getLong(1),
                        rs.getLong(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getBigDecimal(6),
                        rs.getBigDecimal(7),
                        rs.getBigDecimal(8),
                        rs.getBigDecimal(9),
                        rs.getBigDecimal(10),
                        rs.getBigDecimal(11),
                        rs.getBigDecimal(12),
                        rs.getString(13),
                        rs.getObject(14, Integer.class),
                        rs.getString(15)))
            .list();
    return new AutomaticRebalanceResult(date, type.name(), accounts, items);
  }

  private Long latestDecision(LocalDate date) {
    return jdbc.sql(
            "SELECT \"INV_DEC_ID\" FROM \"TB_INV_DEC\" WHERE \"BASE_DT\"=:day AND \"LATEST_YN\"='Y' ORDER BY \"CALC_SEQ\" DESC LIMIT 1")
        .param("day", date)
        .query(Long.class)
        .optional()
        .orElseThrow(() -> new IllegalStateException("8단계 최신 투자판단이 없습니다."));
  }

  private List<Account> accounts() {
    return jdbc.sql(
            "SELECT \"ACCT_ID\",\"ACCT_TP\",COALESCE(\"CASH_AMT\",0),COALESCE(\"RSV_CASH_AMT\",0),COALESCE(\"TGT_CASH_WGT\",20) FROM \"TB_ACCT\" WHERE \"DEL_YN\"='N' ORDER BY \"DISP_SEQ\"")
        .query(
            (rs, n) ->
                new Account(
                    rs.getLong(1),
                    rs.getString(2),
                    rs.getBigDecimal(3),
                    rs.getBigDecimal(4),
                    rs.getBigDecimal(5)))
        .list();
  }

  private List<Position> positions(Long account, Long decision) {
    return jdbc.sql(
            """
  SELECT h."HOLD_ID",h."STK_ID",s."STK_CD",s."STK_NM",COALESCE(h."EVL_AMT",0),COALESCE(h."CUR_WGT",0),COALESCE(h."TGT_WGT",0),s."FUND_DMG_YN",COALESCE(r."MIN_BUY_AMT",0),COALESCE(d."REG_BUY_AMT",r."MIN_BUY_AMT",0)
  FROM "TB_HOLD" h JOIN "TB_STK" s ON s."STK_ID"=h."STK_ID" LEFT JOIN "TB_REG_BUY" r ON r."ACCT_ID"=h."ACCT_ID" AND r."STK_ID"=h."STK_ID" AND r."DEL_YN"='N' LEFT JOIN "TB_STK_DEC" d ON d."INV_DEC_ID"=:decision AND d."ACCT_ID"=h."ACCT_ID" AND d."STK_ID"=h."STK_ID"
  WHERE h."ACCT_ID"=:account AND h."USE_YN"='Y' AND h."DEL_YN"='N' ORDER BY s."STK_CD"
  """)
        .param("decision", decision)
        .param("account", account)
        .query(
            (rs, n) ->
                new Position(
                    rs.getLong(1),
                    rs.getLong(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getBigDecimal(5),
                    rs.getBigDecimal(6),
                    rs.getBigDecimal(7),
                    "Y".equals(rs.getString(8)),
                    rs.getBigDecimal(9),
                    rs.getBigDecimal(10)))
        .list();
  }

  private Long insertParent(
      LocalDate day,
      RebalanceType type,
      Long decision,
      Account a,
      RebalanceCalculationService.AccountAmounts x,
      Integer sequence) {
    return jdbc.sql(
            "INSERT INTO \"TB_REBAL\"(\"BASE_DT\",\"CALC_SEQ\",\"ACCT_ID\",\"INV_DEC_ID\",\"REBAL_TP\",\"REBAL_STS\",\"TOT_AST_AMT\",\"CASH_AMT\",\"RSV_CASH_AMT\",\"CUR_CASH_WGT\",\"TGT_CASH_WGT\",\"CASH_GAP_AMT\",\"NEW_CASH_AMT\",\"BUY_BGT_AMT\",\"SELL_TGT_AMT\",\"CONF_RT\",\"KEY_RSN\",\"LATEST_YN\") VALUES(:day,:seq,:account,:decision,:type,'CALCULATED',:total,:cash,:reserve,:currentWeight,:targetWeight,:gap,0,:budget,:sell,80,'자동 투자판단 기반 리밸런싱 계산','Y') RETURNING \"REBAL_ID\"")
        .param("day", day)
        .param("seq", sequence)
        .param("account", a.id())
        .param("decision", decision)
        .param("type", type.name())
        .param("total", x.totalAssetAmount())
        .param("cash", a.cash())
        .param("reserve", a.reserve())
        .param("currentWeight", x.currentCashWeight())
        .param("targetWeight", a.targetCashWeight())
        .param("gap", x.cashGapAmount())
        .param("budget", x.buyBudgetAmount())
        .param("sell", x.cashGapAmount().negate().max(BigDecimal.ZERO))
        .query(Long.class)
        .single();
  }

  private Long insertItem(
      Long parent,
      LocalDate day,
      Account a,
      Position p,
      RebalanceCalculationService.ItemAmounts x,
      BigDecimal currentRegular,
      BigDecimal newRegular,
      BigDecimal change,
      String action,
      int priority,
      String reason,
      BigDecimal buy,
      BigDecimal sell) {
    return jdbc.sql(
            "INSERT INTO \"TB_REBAL_ITEM\"(\"REBAL_ID\",\"ACCT_ID\",\"STK_ID\",\"HOLD_ID\",\"BASE_DT\",\"CUR_AMT\",\"CUR_WGT\",\"TGT_WGT\",\"TGT_AMT\",\"WGT_GAP_AMT\",\"WGT_STS\",\"REBAL_ACT\",\"BUY_NEED_AMT\",\"SELL_NEED_AMT\",\"RCMD_BUY_AMT\",\"RCMD_SELL_AMT\",\"CUR_REG_BUY_AMT\",\"NEW_REG_BUY_AMT\",\"REG_BUY_CHG_AMT\",\"PRIO_NO\",\"FUND_DMG_YN\",\"TRADE_LIMIT_YN\",\"EXEC_YN\",\"ITEM_STS\",\"DEC_RSN\") VALUES(:parent,:account,:stock,:hold,:day,:current,:currentWeight,:targetWeight,:target,:gap,:status,:action,:buyNeed,:sellNeed,:buy,:sell,:currentRegular,:newRegular,:change,:priority,:fund,'N','N','READY',:reason) RETURNING \"REBAL_ITEM_ID\"")
        .param("parent", parent)
        .param("account", a.id())
        .param("stock", p.stockId())
        .param("hold", p.holdId())
        .param("day", day)
        .param("current", p.amount())
        .param("currentWeight", p.currentWeight())
        .param("targetWeight", percent(p.targetWeight()))
        .param("target", x.targetAmount())
        .param("gap", x.weightGapAmount())
        .param("status", x.weightStatus().name())
        .param("action", action)
        .param("buyNeed", x.buyNeedAmount())
        .param("sellNeed", x.sellNeedAmount())
        .param("buy", buy)
        .param("sell", sell)
        .param("currentRegular", currentRegular)
        .param("newRegular", newRegular)
        .param("change", change)
        .param("priority", priority)
        .param("fund", p.fundamentalDamaged() ? "Y" : "N")
        .param("reason", reason)
        .query(Long.class)
        .single();
  }

  private String regularAction(BigDecimal current, BigDecimal next) {
    int compare = next.compareTo(current);
    return compare > 0
        ? "INCREASE_REGULAR_BUY"
        : compare < 0 ? (next.signum() == 0 ? "PAUSE_REGULAR_BUY" : "REDUCE_REGULAR_BUY") : "HOLD";
  }

  private BigDecimal percent(BigDecimal value) {
    if (value == null) return BigDecimal.ZERO;
    return value.compareTo(BigDecimal.ONE) <= 0 ? value.multiply(HUNDRED) : value;
  }

  private record Account(
      Long id, String type, BigDecimal cash, BigDecimal reserve, BigDecimal targetCashWeight) {}

  private record Position(
      Long holdId,
      Long stockId,
      String code,
      String name,
      BigDecimal amount,
      BigDecimal currentWeight,
      BigDecimal targetWeight,
      boolean fundamentalDamaged,
      BigDecimal currentRegular,
      BigDecimal recommendedRegular) {
    BigDecimal weightLimit(BigDecimal total) {
      return total.multiply(percentValue(targetWeight)).divide(HUNDRED, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal percentValue(BigDecimal v) {
      return v == null
          ? BigDecimal.ZERO
          : v.compareTo(BigDecimal.ONE) <= 0 ? v.multiply(HUNDRED) : v;
    }
  }
}
