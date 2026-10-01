package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanum.investment.marketdata.domain.KofiaCustomerScaleVariant.Stage;
import com.nanum.investment.marketdata.infrastructure.KofiaClient;
import com.nanum.investment.marketdata.infrastructure.KofiaCustomerScaleRepository;
import com.nanum.investment.marketdata.infrastructure.KofiaCustomerScaleRepository.CodeValue;
import com.nanum.investment.marketdata.infrastructure.KofiaCustomerScaleRepository.VariantSeed;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KofiaCustomerScaleServiceTest {
  @Test
  void createsAggregateRegionKindAndSellerVariantsForBothOfferingsAndMetrics() {
    KofiaCustomerScaleRepository repository = mock(KofiaCustomerScaleRepository.class);
    KofiaClient client = mock(KofiaClient.class);
    KofiaCustomerScaleJobRunner runner = mock(KofiaCustomerScaleJobRunner.class);
    KofiaCustomerScaleService service = new KofiaCustomerScaleService(client, repository, runner);
    when(repository.activeCodes("T1100"))
        .thenReturn(
            List.of(
                new CodeValue("5", "1"),
                new CodeValue("7", "2"),
                new CodeValue("A", "3"),
                new CodeValue("L", "4"),
                new CodeValue("M", "5"),
                new CodeValue("P", "6"),
                new CodeValue("R", "7"),
                new CodeValue("V", "8")));
    when(repository.activeSellers())
        .thenReturn(List.of(new CodeValue("S001", "A"), new CodeValue("S002", "B")));
    when(repository.syncGeneratedVariants(anyList())).thenReturn(60);

    var result = service.syncVariants();

    assertThat(result.totalCount()).isEqualTo(60);
    assertThat(result.aggregateCount()).isEqualTo(4);
    assertThat(result.regionCount()).isEqualTo(16);
    assertThat(result.fundKindCount()).isEqualTo(32);
    assertThat(result.sellerCount()).isEqualTo(8);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<VariantSeed>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).syncGeneratedVariants(captor.capture());
    assertThat(captor.getValue()).hasSize(60);
    assertThat(captor.getValue()).extracting(VariantSeed::stage).contains(Stage.AGGREGATE);
    assertThat(captor.getValue()).extracting(VariantSeed::offeringTypeCode).containsOnly("1", "2");
    assertThat(captor.getValue()).extracting(VariantSeed::metricTypeCode).containsOnly("1", "2");
    assertThat(captor.getValue()).extracting(VariantSeed::parameterHash).doesNotHaveDuplicates();
  }
}
