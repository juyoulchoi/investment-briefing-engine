package com.nanum.investment.briefing.api.request;

import com.nanum.investment.marketdata.domain.DataSourceCode;
import com.nanum.investment.marketdata.domain.IndexType;
import jakarta.validation.constraints.*;

public record IndexSaveRequest(
    @NotBlank @Size(max = 30) String indexCode,
    @NotBlank @Size(max = 150) String indexName,
    @Size(max = 200) String indexEnglishName,
    @NotNull IndexType indexType,
    @Size(max = 30) String marketCode,
    @NotBlank @Size(max = 10) String countryCode,
    @NotBlank @Size(max = 10) String currencyCode,
    @NotNull DataSourceCode dataSourceCode,
    @Size(max = 50) String sourceSymbol,
    @Pattern(regexp = "[YN]") String defaultYn) {}
