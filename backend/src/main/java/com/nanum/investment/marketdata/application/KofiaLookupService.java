package com.nanum.investment.marketdata.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.common.exception.ErrorCode;
import com.nanum.investment.marketdata.infrastructure.KofiaCatalogClient;
import com.nanum.investment.marketdata.infrastructure.KofiaLookupRepository;
import com.nanum.investment.marketdata.infrastructure.KofiaRequestRateLimiter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class KofiaLookupService {
  public static final String CASH_FLOW_TABLE = "TM_CASHFLOWSTUT ";
  public static final String CUSTOMER_TYPE_SALES_TABLE = "TM_CUSTTYPSALESTUT";
  private static final List<String> TYPES = List.of("1", "2");
  private static final List<String> COMPANY_TABLES =
      List.of(CASH_FLOW_TABLE, CUSTOMER_TYPE_SALES_TABLE);

  private final KofiaCatalogClient client;
  private final KofiaLookupRepository repository;
  private final KofiaRequestRateLimiter limiter;

  public KofiaLookupService(
      KofiaCatalogClient client,
      KofiaLookupRepository repository,
      KofiaRequestRateLimiter limiter) {
    this.client = client;
    this.repository = repository;
    this.limiter = limiter;
  }

  public CollectionView collectAll() {
    List<ScopeView> scopes = new ArrayList<>();
    for (String marketType : TYPES) {
      limiter.acquire(0);
      JsonNode response = client.businesses(marketType);
      int count = repository.saveBusinesses(marketType, response);
      scopes.add(new ScopeView("BUSINESS", "tmpV1=" + marketType, count));
    }
    for (String tableName : COMPANY_TABLES) {
      for (String companyType : TYPES) {
        limiter.acquire(0);
        JsonNode response = client.companies(tableName, companyType);
        int count = repository.saveCompanies(tableName, companyType, response);
        scopes.add(new ScopeView("COMPANY", tableName.trim() + ":tmpV18=" + companyType, count));
      }
    }
    return new CollectionView(scopes.stream().mapToInt(ScopeView::rowCount).sum(), scopes);
  }

  public List<Map<String, Object>> businesses(String marketType) {
    validateType(marketType, "marketType");
    return repository.businesses(marketType);
  }

  public List<Map<String, Object>> companies(String tableName, String companyType) {
    validateType(companyType, "companyType");
    String table = canonicalTable(tableName);
    return repository.companies(table, companyType);
  }

  private void validateType(String value, String field) {
    if (!TYPES.contains(value))
      throw new BusinessException(ErrorCode.INVALID_REQUEST, field + "은 1 또는 2여야 합니다.");
  }

  private String canonicalTable(String value) {
    if (value == null) throw new BusinessException(ErrorCode.INVALID_REQUEST, "tableName이 필요합니다.");
    String canonical = value.trim();
    return COMPANY_TABLES.stream()
        .map(String::trim)
        .filter(canonical::equals)
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.INVALID_REQUEST, "지원하지 않는 KOFIA 회사검색 테이블입니다: " + value));
  }

  public record CollectionView(int totalRowCount, List<ScopeView> scopes) {}

  public record ScopeView(String lookupType, String scope, int rowCount) {}
}
