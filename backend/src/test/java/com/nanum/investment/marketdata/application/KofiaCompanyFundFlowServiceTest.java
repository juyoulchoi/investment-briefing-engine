package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanum.investment.marketdata.domain.KofiaCompanyFundFlowVariant.Stage;
import com.nanum.investment.marketdata.infrastructure.KofiaClient;
import com.nanum.investment.marketdata.infrastructure.KofiaCompanyFundFlowRepository;
import com.nanum.investment.marketdata.infrastructure.KofiaCompanyFundFlowRepository.CodeValue;
import com.nanum.investment.marketdata.infrastructure.KofiaCompanyFundFlowRepository.VariantSeed;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KofiaCompanyFundFlowServiceTest {
  @Test
  void createsFiftyStagedVariants() {
    KofiaCompanyFundFlowRepository repository = mock(KofiaCompanyFundFlowRepository.class);
    KofiaClient client = mock(KofiaClient.class);
    KofiaCompanyFundFlowJobRunner runner = mock(KofiaCompanyFundFlowJobRunner.class);
    KofiaCompanyFundFlowService service =
        new KofiaCompanyFundFlowService(client, repository, runner);
    when(repository.activeCodes("T1111"))
        .thenReturn(
            List.of(
                new CodeValue("01", "1"), new CodeValue("02", "2"),
                new CodeValue("03", "3"), new CodeValue("04", "4"),
                new CodeValue("05", "5"), new CodeValue("06", "6"),
                new CodeValue("07", "7"), new CodeValue("08", "8"),
                new CodeValue("09", "9"), new CodeValue("10", "10"),
                new CodeValue("11", "11"), new CodeValue("12", "12"),
                new CodeValue("13", "13"), new CodeValue("14", "14")));
    when(repository.activeCodes("T1100"))
        .thenReturn(
            List.of(
                new CodeValue("5", "1"), new CodeValue("7", "2"),
                new CodeValue("A", "3"), new CodeValue("L", "4"),
                new CodeValue("M", "5"), new CodeValue("P", "6"),
                new CodeValue("R", "7"), new CodeValue("V", "8")));
    when(repository.syncGeneratedVariants(anyList())).thenReturn(50);

    var result = service.syncVariants();

    assertThat(result.totalCount()).isEqualTo(50);
    assertThat(result.aggregateCount()).isEqualTo(2);
    assertThat(result.offeringCount()).isEqualTo(4);
    assertThat(result.fundTypeCount()).isEqualTo(28);
    assertThat(result.fundKindCount()).isEqualTo(16);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<VariantSeed>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).syncGeneratedVariants(captor.capture());
    assertThat(captor.getValue()).hasSize(50);
    assertThat(captor.getValue()).extracting(VariantSeed::stage).contains(Stage.AGGREGATE);
    assertThat(captor.getValue()).extracting(VariantSeed::etfIncludeYn).containsOnly("Y", "N");
    assertThat(captor.getValue()).extracting(VariantSeed::parameterHash).doesNotHaveDuplicates();
  }
}
