package com.nanum.investment.api;
import com.nanum.investment.domain.*; import jakarta.validation.Valid; import jakarta.validation.constraints.NotEmpty; import java.time.LocalDate; import java.util.List;
public record InvestmentDecisionRequest(LocalDate decisionDate,@Valid MarketSnapshot market,@NotEmpty List<@Valid StockPosition> positions,long existingReservedCash) {}
