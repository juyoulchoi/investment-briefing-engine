package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.marketdata.domain.KofiaDataset;
import java.time.LocalDate;
import java.util.List;

public interface KofiaClient {
  KofiaResponse collect(KofiaDataset dataset, LocalDate from, LocalDate to);

  record KofiaResponse(JsonNode rawResponse, List<KofiaRow> rows) {}

  record KofiaRow(LocalDate baseDate, JsonNode payload) {}
}
