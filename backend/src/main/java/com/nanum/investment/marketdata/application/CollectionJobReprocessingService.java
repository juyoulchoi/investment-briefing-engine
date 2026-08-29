package com.nanum.investment.marketdata.application;

import com.nanum.investment.marketdata.domain.KrxDataset;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class CollectionJobReprocessingService {
  private final JdbcClient jdbc;
  private final KrxCollectionJobRunner krxRunner;
  private final FredMacroJobRunner fredRunner;
  private final KofiaCollectionJobRunner kofiaRunner;

  public CollectionJobReprocessingService(JdbcClient jdbc, KrxCollectionJobRunner krxRunner,
      FredMacroJobRunner fredRunner, KofiaCollectionJobRunner kofiaRunner) {
    this.jdbc = jdbc;
    this.krxRunner = krxRunner;
    this.fredRunner = fredRunner;
    this.kofiaRunner = kofiaRunner;
  }

  @Transactional
  public ReprocessingView retry(String requestedProvider, UUID originalJobId) {
    String provider = requestedProvider.toUpperCase(Locale.ROOT);
    UUID retryJobId = UUID.randomUUID();
    return switch (provider) {
      case "KRX" -> retryKrx(originalJobId, retryJobId);
      case "FRED" -> retryFred(originalJobId, retryJobId);
      case "KOFIA" -> retryKofia(originalJobId, retryJobId);
      default -> throw new IllegalArgumentException("지원하지 않는 공급자입니다: " + requestedProvider);
    };
  }

  private ReprocessingView retryKrx(UUID original, UUID retry) {
    Map<String, Object> job = jdbc.sql("SELECT * FROM \"TB_KRX_CLCT_JOB\" WHERE \"ID\"=:id")
        .param("id", original).query().singleRow();
    int retryNo = nextRetry(job);
    List<String> datasets = jdbc.sql("SELECT \"DATA_CD\" FROM \"TB_KRX_CLCT_JOB_ITEM\" WHERE \"JOB_ID\"=:id AND \"ST\" IN ('COLLECTION_FAILED','NO_DATA_UNEXPECTED','NOT_AUTHORIZED','FAILED')")
        .param("id", original).query(String.class).list();
    requireFailures(datasets.size());
    UUID root = root(job, original);
    jdbc.sql("INSERT INTO \"TB_KRX_CLCT_JOB\"(\"ID\",\"BASE_DT\",\"ST\",\"TOT_CNT\",\"ORIG_JOB_ID\",\"RETRY_ROOT_JOB_ID\",\"RETRY_NO\",\"MAX_RETRY_CNT\") VALUES(:id,:day,'QUEUED',:total,:orig,:root,:retryNo,:max)")
        .param("id", retry).param("day", job.get("BASE_DT")).param("total", datasets.size())
        .param("orig", original).param("root", root).param("retryNo", retryNo).param("max", max(job)).update();
    datasets.forEach(dataset -> jdbc.sql("INSERT INTO \"TB_KRX_CLCT_JOB_ITEM\"(\"JOB_ID\",\"DATA_CD\",\"ST\",\"RETRY_CNT\") VALUES(:job,:dataset,'PENDING',:retryNo)")
        .param("job", retry).param("dataset", dataset).param("retryNo", retryNo).update());
    jdbc.sql("UPDATE \"TB_KRX_CLCT_JOB_ITEM\" SET \"ST\"='RETRIED',\"RETRY_CNT\"=:retryNo WHERE \"JOB_ID\"=:id AND \"ST\" IN ('COLLECTION_FAILED','NO_DATA_UNEXPECTED','NOT_AUTHORIZED','FAILED')")
        .param("id", original).param("retryNo", retryNo).update();
    LocalDate day = ((Date) job.get("BASE_DT")).toLocalDate();
    afterCommit(() -> krxRunner.runNow(retry, day, datasets.stream().map(KrxDataset::valueOf).toList(), 0));
    recordEvent("KRX", original, retry, root, retryNo, max(job), "QUEUED", null);
    return new ReprocessingView("KRX", original, retry, root, retryNo, "QUEUED", datasets.size());
  }

  private ReprocessingView retryFred(UUID original, UUID retry) {
    Map<String, Object> job = jdbc.sql("SELECT * FROM \"TB_FRED_CLCT_JOB\" WHERE \"JOB_ID\"=:id")
        .param("id", original).query().singleRow();
    int retryNo = nextRetry(job);
    List<Long> items = jdbc.sql("SELECT \"SERIES_ID\" FROM \"TB_FRED_CLCT_JOB_ITEM\" WHERE \"JOB_ID\"=:id AND \"STATUS\"='FAILED'")
        .param("id", original).query(Long.class).list();
    requireFailures(items.size());
    UUID root = root(job, original);
    jdbc.sql("INSERT INTO \"TB_FRED_CLCT_JOB\"(\"JOB_ID\",\"FROM_DT\",\"TO_DT\",\"STATUS\",\"SERIES_CDS\",\"REQUEST_INTERVAL_MS\",\"TOTAL_ITEM_CNT\",\"PENDING_ITEM_CNT\",\"ORIG_JOB_ID\",\"RETRY_ROOT_JOB_ID\",\"RETRY_NO\",\"MAX_RETRY_CNT\") VALUES(:id,:from,:to,'QUEUED',:codes,:interval,:total,:total,:orig,:root,:retryNo,:max)")
        .param("id", retry).param("from", job.get("FROM_DT")).param("to", job.get("TO_DT"))
        .param("codes", job.get("SERIES_CDS")).param("interval", job.get("REQUEST_INTERVAL_MS"))
        .param("total", items.size()).param("orig", original).param("root", root)
        .param("retryNo", retryNo).param("max", max(job)).update();
    items.forEach(series -> jdbc.sql("INSERT INTO \"TB_FRED_CLCT_JOB_ITEM\"(\"JOB_ID\",\"SERIES_ID\",\"RETRY_CNT\") VALUES(:job,:series,:retryNo)")
        .param("job", retry).param("series", series).param("retryNo", retryNo).update());
    jdbc.sql("UPDATE \"TB_FRED_CLCT_JOB_ITEM\" SET \"STATUS\"='RETRIED',\"RETRY_CNT\"=:retryNo WHERE \"JOB_ID\"=:id AND \"STATUS\"='FAILED'")
        .param("id", original).param("retryNo", retryNo).update();
    afterCommit(() -> fredRunner.run(retry));
    recordEvent("FRED", original, retry, root, retryNo, max(job), "QUEUED", null);
    return new ReprocessingView("FRED", original, retry, root, retryNo, "QUEUED", items.size());
  }

  private ReprocessingView retryKofia(UUID original, UUID retry) {
    Map<String, Object> job = jdbc.sql("SELECT * FROM \"TB_KOFIA_CLCT_JOB\" WHERE \"JOB_ID\"=:id")
        .param("id", original).query().singleRow();
    int retryNo = nextRetry(job);
    List<Map<String, Object>> items = jdbc.sql("SELECT \"DATASET_CD\",\"FROM_DT\",\"TO_DT\" FROM \"TB_KOFIA_CLCT_JOB_ITEM\" WHERE \"JOB_ID\"=:id AND \"STS\"='FAILED'")
        .param("id", original).query().listOfRows();
    requireFailures(items.size());
    UUID root = root(job, original);
    jdbc.sql("INSERT INTO \"TB_KOFIA_CLCT_JOB\"(\"JOB_ID\",\"FROM_DT\",\"TO_DT\",\"DATASET_CDS\",\"STS\",\"TOTAL_ITEM_CNT\",\"ORIG_JOB_ID\",\"RETRY_ROOT_JOB_ID\",\"RETRY_NO\",\"MAX_RETRY_CNT\") VALUES(:id,:from,:to,:datasets,'QUEUED',:total,:orig,:root,:retryNo,:max)")
        .param("id", retry).param("from", job.get("FROM_DT")).param("to", job.get("TO_DT"))
        .param("datasets", job.get("DATASET_CDS")).param("total", items.size()).param("orig", original)
        .param("root", root).param("retryNo", retryNo).param("max", max(job)).update();
    items.forEach(item -> jdbc.sql("INSERT INTO \"TB_KOFIA_CLCT_JOB_ITEM\"(\"JOB_ID\",\"DATASET_CD\",\"FROM_DT\",\"TO_DT\",\"RETRY_CNT\") VALUES(:job,:dataset,:from,:to,:retryNo)")
        .param("job", retry).param("dataset", item.get("DATASET_CD")).param("from", item.get("FROM_DT"))
        .param("to", item.get("TO_DT")).param("retryNo", retryNo).update());
    jdbc.sql("UPDATE \"TB_KOFIA_CLCT_JOB_ITEM\" SET \"STS\"='RETRIED',\"RETRY_CNT\"=:retryNo WHERE \"JOB_ID\"=:id AND \"STS\"='FAILED'")
        .param("id", original).param("retryNo", retryNo).update();
    afterCommit(() -> kofiaRunner.run(retry));
    recordEvent("KOFIA", original, retry, root, retryNo, max(job), "QUEUED", null);
    return new ReprocessingView("KOFIA", original, retry, root, retryNo, "QUEUED", items.size());
  }

  private int nextRetry(Map<String, Object> job) {
    int next = ((Number) job.get("RETRY_NO")).intValue() + 1;
    if (next > max(job)) throw new IllegalStateException("최대 재처리 횟수를 초과하여 DEAD_LETTER 대상입니다.");
    return next;
  }
  private int max(Map<String, Object> job) { return ((Number) job.get("MAX_RETRY_CNT")).intValue(); }
  private UUID root(Map<String, Object> job, UUID original) {
    Object value = job.get("RETRY_ROOT_JOB_ID");
    return value == null ? original : (UUID) value;
  }
  private void requireFailures(int count) { if (count == 0) throw new IllegalStateException("재처리할 최종 실패 Item이 없습니다."); }
  private void recordEvent(String provider, UUID original, UUID retry, UUID root, int retryNo,
      int maxRetry, String status, String error) {
    jdbc.sql("INSERT INTO \"TB_CLCT_RETRY_EVT\"(\"PROVIDER_CD\",\"ORIG_JOB_ID\",\"RETRY_JOB_ID\",\"RETRY_ROOT_JOB_ID\",\"RETRY_NO\",\"MAX_RETRY_CNT\",\"STATUS\",\"NEXT_RETRY_DTTM\",\"ERROR_MSG\") VALUES(:provider,:orig,:retry,:root,:retryNo,:max,:status,CURRENT_TIMESTAMP,:error)")
        .param("provider", provider).param("orig", original).param("retry", retry)
        .param("root", root).param("retryNo", retryNo).param("max", maxRetry)
        .param("status", status).param("error", error).update();
  }

  @Transactional
  public void markPermanentFailure(String requestedProvider, UUID jobId, String reason) {
    String provider = requestedProvider.toUpperCase(Locale.ROOT);
    String table;
    String jobColumn;
    String statusColumn;
    switch (provider) {
      case "KRX" -> { table = "TB_KRX_CLCT_JOB_ITEM"; jobColumn = "JOB_ID"; statusColumn = "ST"; }
      case "FRED" -> { table = "TB_FRED_CLCT_JOB_ITEM"; jobColumn = "JOB_ID"; statusColumn = "STATUS"; }
      case "KOFIA" -> { table = "TB_KOFIA_CLCT_JOB_ITEM"; jobColumn = "JOB_ID"; statusColumn = "STS"; }
      default -> throw new IllegalArgumentException("지원하지 않는 공급자입니다: " + requestedProvider);
    }
    jdbc.sql("UPDATE \"" + table + "\" SET \"" + statusColumn + "\"='PERMANENT_FAILED' WHERE \"" + jobColumn + "\"=:id AND \"" + statusColumn + "\" IN ('FAILED','RETRY_WAIT')")
        .param("id", jobId).update();
    recordEvent(provider, jobId, null, jobId, 0, 0, "PERMANENT_FAILED", reason);
  }
  private void afterCommit(Runnable action) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override public void afterCommit() { action.run(); }
    });
  }

  public record ReprocessingView(String provider, UUID originalJobId, UUID retryJobId,
      UUID retryRootJobId, int retryNumber, String status, int itemCount) {}
}
