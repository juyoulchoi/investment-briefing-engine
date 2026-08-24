package com.nanum.investment.marketdata.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class FredBondYieldCollector implements BondYieldCollector {
  private static final Map<String, BondInfo> BONDS =
      Map.of(
          "DGS2",
          new BondInfo("미국 국채 2년", 24),
          "DGS10",
          new BondInfo("미국 국채 10년", 120),
          "DGS30",
          new BondInfo("미국 국채 30년", 360),
          "DFII10",
          new BondInfo("미국 물가연동국채 실질금리 10년", 120));
  private final FredClient client;

  public FredBondYieldCollector(FredClient client) {
    this.client = client;
  }

  @Override
  public Yield collect(String bondCode, LocalDate date) {
    return collectRange(bondCode, date, date).stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("해당 날짜의 FRED 채권금리가 없습니다."));
  }

  public List<Yield> collectRange(String requestedCode, LocalDate from, LocalDate to) {
    String code = requestedCode == null ? "" : requestedCode.trim().toUpperCase();
    BondInfo info = BONDS.get(code);
    if (info == null) throw new IllegalArgumentException("지원하지 않는 FRED 채권 코드입니다: " + code);
    List<Yield> result = new ArrayList<>();
    for (FredClient.Observation item : client.observations(code, from, to, "lin", "avg")) {
      if (item.value() == null) continue;
      result.add(
          new Yield(
              item.observationDate(),
              code,
              info.name(),
              "US",
              info.months(),
              new BigDecimal(item.value().toPlainString()),
              "FRED"));
    }
    return result;
  }

  public Set<String> supportedCodes() {
    return BONDS.keySet();
  }

  private record BondInfo(String name, Integer months) {}
}
