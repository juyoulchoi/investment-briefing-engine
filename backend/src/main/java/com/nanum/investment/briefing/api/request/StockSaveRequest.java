package com.nanum.investment.briefing.api.request;

import com.nanum.investment.common.domain.AssetType;
import com.nanum.investment.common.domain.StockGrade;
import jakarta.validation.constraints.*;

public record StockSaveRequest(
    @NotBlank @Size(max = 30) String stockCode,
    @NotBlank @Size(max = 150) String stockName,
    @Size(max = 200) String stockEnglishName,
    @NotBlank @Size(max = 30) String marketCode,
    @NotBlank @Size(max = 10) String countryCode,
    @NotBlank @Size(max = 10) String currencyCode,
    @NotNull AssetType assetType,
    @NotNull StockGrade stockGrade,
    Long baseIndexId,
    @Size(max = 30) String sectorCode,
    @Size(max = 100) String sectorName,
    @Size(max = 30) String industryCode,
    @Size(max = 100) String industryName,
    @Pattern(regexp = "[YN]") String regularBuyYn,
    @Pattern(regexp = "[YN]") String additionalBuyYn,
    @Pattern(regexp = "[YN]") String rebuyYn) {}
