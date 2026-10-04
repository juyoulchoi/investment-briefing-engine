package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanum.investment.marketdata.domain.KofiaFundFlowVariant.Stage;
import com.nanum.investment.marketdata.infrastructure.KofiaClient;
import com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository;
import com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository.CodeValue;
import com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository.VariantSeed;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KofiaFundFlowServiceTest {
  @Test
  void createsAggregateTypeKindAndManagerVariantsForBothEtfOptions() {
    KofiaFundFlowRepository repository = mock(KofiaFundFlowRepository.class);
    KofiaClient client = mock(KofiaClient.class);
    KofiaFundFlowJobRunner runner = mock(KofiaFundFlowJobRunner.class);
    KofiaFundFlowService service = new KofiaFundFlowService(client, repository, runner);
    when(repository.activeCodes("T1111"))
        .thenReturn(
            List.of(
                new CodeValue("01", "1"),
                new CodeValue("02", "2"),
                new CodeValue("03", "3"),
                new CodeValue("04", "4"),
                new CodeValue("05", "5"),
                new CodeValue("06", "6"),
                new CodeValue("07", "7"),
                new CodeValue("08", "8"),
                new CodeValue("09", "9"),
                new CodeValue("10", "10"),
                new CodeValue("11", "11"),
                new CodeValue("12", "12"),
                new CodeValue("13", "13"),
                new CodeValue("14", "14")));
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
    when(repository.activeManagers())
        .thenReturn(List.of(new CodeValue("A001", "A"), new CodeValue("A002", "B")));
    when(repository.syncGeneratedVariants(anyList())).thenReturn(100);

    var result = service.syncVariants();

    assertThat(result.totalCount()).isEqualTo(100);
    assertThat(result.aggregateCount()).isEqualTo(4);
    assertThat(result.fundTypeCount()).isEqualTo(56);
    assertThat(result.fundKindCount()).isEqualTo(32);
    assertThat(result.managerCount()).isEqualTo(8);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<VariantSeed>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).syncGeneratedVariants(captor.capture());
    assertThat(captor.getValue()).hasSize(100);
    assertThat(captor.getValue()).extracting(VariantSeed::stage).contains(Stage.AGGREGATE);
    assertThat(captor.getValue()).extracting(VariantSeed::etfIncludeYn).containsOnly("Y", "N");
    assertThat(captor.getValue()).extracting(VariantSeed::parameterHash).doesNotHaveDuplicates();
  }
}
