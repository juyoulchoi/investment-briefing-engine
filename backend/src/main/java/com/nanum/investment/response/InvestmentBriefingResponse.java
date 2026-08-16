package com.nanum.investment.response;

import java.time.LocalDate;
import java.util.List;

public record InvestmentBriefingResponse(
    LocalDate briefingDate,
    String title,
    List<BriefingItemResponse> items,
    BriefingItemResponse finalJudgment) {}
