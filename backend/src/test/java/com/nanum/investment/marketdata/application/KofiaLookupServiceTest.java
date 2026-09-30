package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.marketdata.infrastructure.KofiaCatalogClient;
import com.nanum.investment.marketdata.infrastructure.KofiaLookupRepository;
import com.nanum.investment.marketdata.infrastructure.KofiaRequestRateLimiter;
import org.junit.jupiter.api.Test;

class KofiaLookupServiceTest {
  @Test
  void collectsMarketAndCompanyTypesOneAndTwoSeparately() throws Exception {
    KofiaCatalogClient client = mock(KofiaCatalogClient.class);
    KofiaLookupRepository repository = mock(KofiaLookupRepository.class);
    KofiaRequestRateLimiter limiter = mock(KofiaRequestRateLimiter.class);
    KofiaLookupService service = new KofiaLookupService(client, repository, limiter);
    JsonNode response = new ObjectMapper().readTree("{\"success\":true,\"dsList\":[]}");

    when(client.businesses("1")).thenReturn(response);
    when(client.businesses("2")).thenReturn(response);
    when(client.companies(KofiaLookupService.CASH_FLOW_TABLE, "1")).thenReturn(response);
    when(client.companies(KofiaLookupService.CASH_FLOW_TABLE, "2")).thenReturn(response);
    when(client.companies(KofiaLookupService.CUSTOMER_TYPE_SALES_TABLE, "1")).thenReturn(response);
    when(client.companies(KofiaLookupService.CUSTOMER_TYPE_SALES_TABLE, "2")).thenReturn(response);

    var result = service.collectAll();

    assertThat(result.scopes())
        .extracting(KofiaLookupService.ScopeView::scope)
        .containsExactly(
            "tmpV1=1",
            "tmpV1=2",
            "TM_CASHFLOWSTUT:tmpV18=1",
            "TM_CASHFLOWSTUT:tmpV18=2",
            "TM_CUSTTYPSALESTUT:tmpV18=1",
            "TM_CUSTTYPSALESTUT:tmpV18=2");
    verify(repository).saveBusinesses("1", response);
    verify(repository).saveBusinesses("2", response);
    verify(repository).saveCompanies(KofiaLookupService.CASH_FLOW_TABLE, "1", response);
    verify(repository).saveCompanies(KofiaLookupService.CASH_FLOW_TABLE, "2", response);
    verify(repository).saveCompanies(KofiaLookupService.CUSTOMER_TYPE_SALES_TABLE, "1", response);
    verify(repository).saveCompanies(KofiaLookupService.CUSTOMER_TYPE_SALES_TABLE, "2", response);
  }
}
