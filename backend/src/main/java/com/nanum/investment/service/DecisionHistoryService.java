package com.nanum.investment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.api.InvestmentDecisionRequest;
import com.nanum.investment.domain.PortfolioDecision;
import com.nanum.investment.domain.StockDecision;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecisionHistoryService {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public DecisionHistoryService(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Transactional
    public void save(InvestmentDecisionRequest request, PortfolioDecision decision) {
        Long id = jdbc.sql("""
            INSERT INTO tb_investment_decision (
              decision_date, market_regime, market_score, sentiment_phase, sentiment_risk_score,
              total_minimum_buy_amount, total_recommended_buy_amount, newly_reserved_cash,
              available_additional_buy_cash, request_payload, result_payload
            ) VALUES (:date, :regime, :marketScore, :phase, :riskScore, :minimum, :recommended,
                      :reserved, :available, CAST(:request AS jsonb), CAST(:result AS jsonb))
            RETURNING id
            """)
            .param("date", decision.decisionDate())
            .param("regime", decision.market().regime().name())
            .param("marketScore", decision.market().marketScore())
            .param("phase", decision.market().sentiment().phase().name())
            .param("riskScore", decision.market().sentiment().sentimentRiskScore())
            .param("minimum", decision.totalMinimumBuyAmount())
            .param("recommended", decision.totalRecommendedBuyAmount())
            .param("reserved", decision.newlyReservedCash())
            .param("available", decision.availableAdditionalBuyCash())
            .param("request", toJson(request))
            .param("result", toJson(decision))
            .query(Long.class).single();

        for (StockDecision stock : decision.stockDecisions()) {
            jdbc.sql("""
                INSERT INTO tb_investment_stock_decision (
                  investment_decision_id, account, stock_code, stock_name, action_signal,
                  multiplier, minimum_buy_amount, recommended_buy_amount, reserved_cash, reasons
                ) VALUES (:decisionId, :account, :code, :name, :action, :multiplier,
                          :minimum, :recommended, :reserved, CAST(:reasons AS jsonb))
                """)
                .param("decisionId", id).param("account", stock.account())
                .param("code", stock.code()).param("name", stock.name())
                .param("action", stock.action().name()).param("multiplier", stock.multiplier())
                .param("minimum", stock.minimumBuyAmount()).param("recommended", stock.recommendedBuyAmount())
                .param("reserved", stock.reservedCash()).param("reasons", toJson(stock.reasons()))
                .update();
        }
    }

    private String toJson(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("의사결정 이력 JSON 변환에 실패했습니다.", e); }
    }
}
