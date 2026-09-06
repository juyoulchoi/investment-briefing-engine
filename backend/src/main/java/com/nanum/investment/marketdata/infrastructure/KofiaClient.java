package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.marketdata.domain.KofiaDataset;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface KofiaClient {
  KofiaResponse collect(KofiaDataset dataset, LocalDate from, LocalDate to);

  record KofiaResponse(
      JsonNode rawResponse, Map<String, Object> requestParameters, List<KofiaRow> rows) {}

  record KofiaRow(
      LocalDate baseDate,
      String rowKey,
      String rowType,
      String entityCode,
      String entityName,
      JsonNode payload) {}
}
