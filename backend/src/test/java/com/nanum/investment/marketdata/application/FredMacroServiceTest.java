package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.marketdata.infrastructure.*;
import com.nanum.investment.marketdata.infrastructure.FredMacroRepository.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.simple.JdbcClient;

class FredMacroServiceTest {
  private FredClient client;
  private FredMacroRepository repository;
  private FredMacroJobRunner runner;
  private FredMacroService service;

  @BeforeEach
  void setUp() {
    client = mock(FredClient.class);
    repository = mock(FredMacroRepository.class);
    runner = mock(FredMacroJobRunner.class);
    service = new FredMacroService(client, repository, runner, mock(JdbcClient.class));
  }

  @Test
  void registersSeriesUsingFredMetadata() {
    var metadata =
        new FredClient.SeriesMetadata(
            "CPIAUCSL",
            "Consumer Price Index",
            LocalDate.of(1947, 1, 1),
            LocalDate.of(2026, 7, 1),
            "Monthly",
            "M",
            "Index 1982-1984=100",
            "Seasonally Adjusted",
            OffsetDateTime.now());
    var command =
        new SeriesCommand(
            "미국 CPI",
            "INFLATION",
            "US",
            "lin",
            "avg",
            "MONTHLY",
            730,
            "LATEST_ONLY",
            "US_CPI",
            "Y");
    when(client.metadata("CPIAUCSL")).thenReturn(metadata);
    when(repository.upsertSeries(metadata, command)).thenReturn(series("CPIAUCSL", true));
    assertThat(service.register(" cpiaucsl ", command).seriesCode()).isEqualTo("CPIAUCSL");
    verify(repository).upsertSeries(metadata, command);
  }

  @Test
  void rejectsFutureCollectionPeriod() {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    assertThatThrownBy(() -> service.startJob(today, today.plusDays(1), List.of(), 250))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("현재 날짜");
  }

  @Test
  void preventsOverlappingActiveJob() {
    var selected = series("UNRATE", true);
    when(repository.series("UNRATE")).thenReturn(selected);
    when(repository.hasActiveOverlap(any(), any(), eq("UNRATE"))).thenReturn(true);
    assertThatThrownBy(
            () ->
                service.startJob(
                    LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1), List.of("UNRATE"), 250))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("겹치는");
    verifyNoInteractions(runner);
  }

  @Test
  void createsAndSubmitsSeriesJob() {
    var selected = series("UNRATE", true);
    when(repository.series("UNRATE")).thenReturn(selected);
    when(repository.hasActiveOverlap(any(), any(), eq("UNRATE"))).thenReturn(false);
    when(repository.job(any()))
        .thenAnswer(
            invocation ->
                new JobView(
                    invocation.getArgument(0),
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 2, 1),
                    "QUEUED",
                    List.of("UNRATE"),
                    250,
                    1,
                    0,
                    0,
                    1,
                    null,
                    LocalDateTime.now(),
                    null,
                    null,
                    List.of()));
    JobView result =
        service.startJob(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1), List.of("UNRATE"), 250);
    assertThat(result.status()).isEqualTo("QUEUED");
    verify(repository).createJob(any(), any(), any(), eq(List.of(selected)), eq(250L));
    verify(runner).run(any());
  }

  private SeriesView series(String code, boolean active) {
    return new SeriesView(
        1,
        code,
        code,
        "MACRO",
        "US",
        "M",
        "Percent",
        "SA",
        LocalDate.of(2000, 1, 1),
        LocalDate.of(2026, 1, 1),
        OffsetDateTime.now(),
        "lin",
        "avg",
        "MONTHLY",
        120,
        "LATEST_ONLY",
        null,
        null,
        null,
        active);
  }
}
