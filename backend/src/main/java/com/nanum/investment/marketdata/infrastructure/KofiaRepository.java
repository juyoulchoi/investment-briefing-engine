package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.marketdata.domain.KofiaDataset;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class KofiaRepository {
  private final JdbcClient jdbc;

  public KofiaRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public int save(
      UUID jobId,
      KofiaDataset dataset,
      LocalDate from,
      LocalDate to,
      JsonNode raw,
      List<KofiaClient.KofiaRow> rows,
      String responseHash) {
    jdbc.sql(
            """
        INSERT INTO "TB_KOFIA_RAW_RSP"("RAW_RSP_ID","JOB_ID","DATASET_CD","REQ_FROM_DT","REQ_TO_DT",
          "HTTP_STS","ROW_CNT","RAW_HASH","PAYLOAD")
        VALUES(:id,:job,:dataset,:from,:to,200,:count,:hash,CAST(:payload AS jsonb))
        """)
        .param("id", UUID.randomUUID())
        .param("job", jobId)
        .param("dataset", dataset.name())
        .param("from", from)
        .param("to", to)
        .param("count", rows.size())
        .param("hash", responseHash)
        .param("payload", raw.toString())
        .update();
    for (KofiaClient.KofiaRow row : rows) {
      String rowHash = KofiaSupport.sha256(row.payload().toString());
      jdbc.sql(
              """
          INSERT INTO "TB_KOFIA_DATA_ROW"("DATASET_CD","BASE_DT","ROW_KEY","PAYLOAD","RAW_HASH")
          VALUES(:dataset,:day,:key,CAST(:payload AS jsonb),:hash)
          ON CONFLICT("DATASET_CD","BASE_DT","ROW_KEY") DO UPDATE SET
            "PAYLOAD"=EXCLUDED."PAYLOAD","RAW_HASH"=EXCLUDED."RAW_HASH","LAST_COLLECT_DTTM"=CURRENT_TIMESTAMP
          """)
          .param("dataset", dataset.name())
          .param("day", row.baseDate())
          .param("key", row.baseDate().toString())
          .param("payload", row.payload().toString())
          .param("hash", rowHash)
          .update();
      if (dataset == KofiaDataset.CREDIT_BALANCE_TREND) saveCreditBalance(row, rowHash);
      if (dataset == KofiaDataset.SECURITIES_LENDING_TREND) saveSecuritiesLending(row, rowHash);
      if (dataset == KofiaDataset.MARKET_FUNDS_TREND) saveMarketFunds(row, rowHash);
    }
    return rows.size();
  }

  private void saveSecuritiesLending(KofiaClient.KofiaRow row, String hash) {
    JsonNode p = row.payload();
    jdbc.sql(
            """
        INSERT INTO "TB_KOFIA_SEC_LEND_DAY"("BASE_DT","ITEM_CD","ITEM_NM","CONTRACT_QTY",
          "REPAY_QTY","BALANCE_QTY","BALANCE_MKT_AMT","RAW_HASH")
        VALUES(:day,'ALL',:name,:contract,:repay,:balanceQty,:balanceAmt,:hash)
        ON CONFLICT("BASE_DT","ITEM_CD") DO UPDATE SET
          "ITEM_NM"=EXCLUDED."ITEM_NM","CONTRACT_QTY"=EXCLUDED."CONTRACT_QTY",
          "REPAY_QTY"=EXCLUDED."REPAY_QTY","BALANCE_QTY"=EXCLUDED."BALANCE_QTY",
          "BALANCE_MKT_AMT"=EXCLUDED."BALANCE_MKT_AMT","RAW_HASH"=EXCLUDED."RAW_HASH",
          "DATA_STS"='FRESH',"COLLECT_DTTM"=CURRENT_TIMESTAMP,
          "UPD_DTTM"=CASE WHEN "TB_KOFIA_SEC_LEND_DAY"."RAW_HASH"<>EXCLUDED."RAW_HASH"
            THEN CURRENT_TIMESTAMP ELSE "TB_KOFIA_SEC_LEND_DAY"."UPD_DTTM" END
        """)
        .param("day", row.baseDate())
        .param("name", p.path("TMPV2").asText("전체"))
        .param("contract", decimal(p, "TMPV3"))
        .param("repay", decimal(p, "TMPV4"))
        .param("balanceQty", decimal(p, "TMPV5"))
        .param("balanceAmt", decimal(p, "TMPV6"))
        .param("hash", hash)
        .update();
  }

  private void saveMarketFunds(KofiaClient.KofiaRow row, String hash) {
    JsonNode p = row.payload();
    jdbc.sql(
            """
        INSERT INTO "TB_KOFIA_MKT_FUND_DAY"("BASE_DT","INVESTOR_DEPOSIT_AMT","DERIV_DEPOSIT_AMT",
          "CUSTOMER_RP_BALANCE_AMT","RECEIVABLE_AMT","FORCED_LIQUIDATION_AMT",
          "FORCED_LIQUIDATION_RT","RAW_HASH")
        VALUES(:day,:v2,:v3,:v4,:v5,:v6,:v7,:hash)
        ON CONFLICT("BASE_DT") DO UPDATE SET
          "INVESTOR_DEPOSIT_AMT"=EXCLUDED."INVESTOR_DEPOSIT_AMT",
          "DERIV_DEPOSIT_AMT"=EXCLUDED."DERIV_DEPOSIT_AMT",
          "CUSTOMER_RP_BALANCE_AMT"=EXCLUDED."CUSTOMER_RP_BALANCE_AMT",
          "RECEIVABLE_AMT"=EXCLUDED."RECEIVABLE_AMT",
          "FORCED_LIQUIDATION_AMT"=EXCLUDED."FORCED_LIQUIDATION_AMT",
          "FORCED_LIQUIDATION_RT"=EXCLUDED."FORCED_LIQUIDATION_RT",
          "RAW_HASH"=EXCLUDED."RAW_HASH","DATA_STS"='FRESH',"COLLECT_DTTM"=CURRENT_TIMESTAMP,
          "UPD_DTTM"=CASE WHEN "TB_KOFIA_MKT_FUND_DAY"."RAW_HASH"<>EXCLUDED."RAW_HASH"
            THEN CURRENT_TIMESTAMP ELSE "TB_KOFIA_MKT_FUND_DAY"."UPD_DTTM" END
        """)
        .param("day", row.baseDate())
        .param("v2", decimal(p, "TMPV2"))
        .param("v3", decimal(p, "TMPV3"))
        .param("v4", decimal(p, "TMPV4"))
        .param("v5", decimal(p, "TMPV5"))
        .param("v6", decimal(p, "TMPV6"))
        .param("v7", decimal(p, "TMPV7"))
        .param("hash", hash)
        .update();
  }

  private void saveCreditBalance(KofiaClient.KofiaRow row, String hash) {
    JsonNode p = row.payload();
    jdbc.sql(
            """
        INSERT INTO "TB_KOFIA_CRDT_BAL_DAY"("BASE_DT","CRDT_LOAN_TOT_AMT","KOSPI_CRDT_LOAN_AMT",
          "KOSDAQ_CRDT_LOAN_AMT","STK_LOAN_TOT_AMT","KOSPI_STK_LOAN_AMT","KOSDAQ_STK_LOAN_AMT",
          "ETC_STK_LOAN_AMT","SECU_COLLATERAL_LOAN_AMT","RAW_HASH")
        VALUES(:day,:v2,:v3,:v4,:v5,:v6,:v7,:v8,:v9,:hash)
        ON CONFLICT("BASE_DT") DO UPDATE SET
          "CRDT_LOAN_TOT_AMT"=EXCLUDED."CRDT_LOAN_TOT_AMT",
          "KOSPI_CRDT_LOAN_AMT"=EXCLUDED."KOSPI_CRDT_LOAN_AMT",
          "KOSDAQ_CRDT_LOAN_AMT"=EXCLUDED."KOSDAQ_CRDT_LOAN_AMT",
          "STK_LOAN_TOT_AMT"=EXCLUDED."STK_LOAN_TOT_AMT",
          "KOSPI_STK_LOAN_AMT"=EXCLUDED."KOSPI_STK_LOAN_AMT",
          "KOSDAQ_STK_LOAN_AMT"=EXCLUDED."KOSDAQ_STK_LOAN_AMT",
          "ETC_STK_LOAN_AMT"=EXCLUDED."ETC_STK_LOAN_AMT",
          "SECU_COLLATERAL_LOAN_AMT"=EXCLUDED."SECU_COLLATERAL_LOAN_AMT",
          "RAW_HASH"=EXCLUDED."RAW_HASH","DATA_STS"='FRESH',"COLLECT_DTTM"=CURRENT_TIMESTAMP,
          "UPD_DTTM"=CASE WHEN "TB_KOFIA_CRDT_BAL_DAY"."RAW_HASH"<>EXCLUDED."RAW_HASH"
            THEN CURRENT_TIMESTAMP ELSE "TB_KOFIA_CRDT_BAL_DAY"."UPD_DTTM" END
        """)
        .param("day", row.baseDate())
        .param("v2", decimal(p, "TMPV2"))
        .param("v3", decimal(p, "TMPV3"))
        .param("v4", decimal(p, "TMPV4"))
        .param("v5", decimal(p, "TMPV5"))
        .param("v6", decimal(p, "TMPV6"))
        .param("v7", decimal(p, "TMPV7"))
        .param("v8", decimal(p, "TMPV8"))
        .param("v9", decimal(p, "TMPV9"))
        .param("hash", hash)
        .update();
  }

  private BigDecimal decimal(JsonNode row, String field) {
    JsonNode value = row.path(field);
    if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) return null;
    return new BigDecimal(value.asText().replace(",", ""));
  }

  public List<Map<String, Object>> creditBalances(LocalDate from, LocalDate to, int limit) {
    return jdbc.sql(
            """
        SELECT "BASE_DT" base_date,"CRDT_LOAN_TOT_AMT" credit_loan_total_amount,
          "KOSPI_CRDT_LOAN_AMT" kospi_credit_loan_amount,"KOSDAQ_CRDT_LOAN_AMT" kosdaq_credit_loan_amount,
          "STK_LOAN_TOT_AMT" stock_loan_total_amount,"KOSPI_STK_LOAN_AMT" kospi_stock_loan_amount,
          "KOSDAQ_STK_LOAN_AMT" kosdaq_stock_loan_amount,"ETC_STK_LOAN_AMT" other_stock_loan_amount,
          "SECU_COLLATERAL_LOAN_AMT" securities_collateral_loan_amount,"UNIT_CD" unit_code,
          "DATA_STS" data_status,"COLLECT_DTTM" collected_at,"UPD_DTTM" updated_at
        FROM "TB_KOFIA_CRDT_BAL_DAY" WHERE "BASE_DT" BETWEEN :from AND :to
        ORDER BY "BASE_DT" DESC LIMIT :limit
        """)
        .param("from", from)
        .param("to", to)
        .param("limit", Math.min(Math.max(limit, 1), 10000))
        .query()
        .listOfRows();
  }

  public synchronized void createJob(
      UUID id, LocalDate from, LocalDate to, List<KofiaDataset> datasets) {
    String codes =
        datasets.stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElseThrow();
    boolean overlap =
        jdbc.sql(
                """
        SELECT EXISTS(SELECT 1 FROM "TB_KOFIA_CLCT_JOB" WHERE "STS" IN ('QUEUED','RUNNING')
          AND "FROM_DT"<=:to AND "TO_DT">=:from AND "DATASET_CDS"=:codes)
        """)
            .param("from", from)
            .param("to", to)
            .param("codes", codes)
            .query(Boolean.class)
            .single();
    if (overlap) throw new IllegalStateException("동일 Dataset의 기간이 겹치는 활성 KOFIA 수집 Job이 있습니다.");
    int itemCount = 0;
    for (KofiaDataset ignored : datasets)
      for (LocalDate cursor = from; !cursor.isAfter(to); cursor = cursor.plusDays(90)) itemCount++;
    jdbc.sql(
            """
        INSERT INTO "TB_KOFIA_CLCT_JOB"("JOB_ID","FROM_DT","TO_DT","DATASET_CDS","STS","TOTAL_ITEM_CNT")
        VALUES(:id,:from,:to,:codes,'QUEUED',:count)
        """)
        .param("id", id)
        .param("from", from)
        .param("to", to)
        .param("codes", codes)
        .param("count", itemCount)
        .update();
    for (KofiaDataset dataset : datasets) {
      for (LocalDate cursor = from; !cursor.isAfter(to); cursor = cursor.plusDays(90)) {
        LocalDate chunkTo = cursor.plusDays(89).isAfter(to) ? to : cursor.plusDays(89);
        jdbc.sql(
                """
            INSERT INTO "TB_KOFIA_CLCT_JOB_ITEM"("JOB_ID","DATASET_CD","FROM_DT","TO_DT")
            VALUES(:job,:dataset,:from,:to)
            """)
            .param("job", id)
            .param("dataset", dataset.name())
            .param("from", cursor)
            .param("to", chunkTo)
            .update();
      }
    }
  }

  public boolean markRunning(UUID id) {
    return jdbc.sql(
                "UPDATE \"TB_KOFIA_CLCT_JOB\" SET \"STS\"='RUNNING',\"START_DTTM\"=COALESCE(\"START_DTTM\",CURRENT_TIMESTAMP) WHERE \"JOB_ID\"=:id AND \"STS\" IN ('QUEUED','FAILED','COMPLETED_WITH_ERRORS')")
            .param("id", id)
            .update()
        > 0;
  }

  public PendingItem nextPending(UUID id) {
    return jdbc.sql(
            """
        SELECT "ITEM_ID","DATASET_CD","FROM_DT","TO_DT" FROM "TB_KOFIA_CLCT_JOB_ITEM"
        WHERE "JOB_ID"=:id AND "STS"='PENDING' ORDER BY "FROM_DT","DATASET_CD" LIMIT 1
        """)
        .param("id", id)
        .query(
            (rs, n) ->
                new PendingItem(
                    rs.getLong(1),
                    KofiaDataset.valueOf(rs.getString(2)),
                    rs.getDate(3).toLocalDate(),
                    rs.getDate(4).toLocalDate()))
        .optional()
        .orElse(null);
  }

  public void markItemRunning(long itemId) {
    jdbc.sql(
            "UPDATE \"TB_KOFIA_CLCT_JOB_ITEM\" SET \"STS\"='RUNNING',\"START_DTTM\"=CURRENT_TIMESTAMP,\"ERROR_MSG\"=NULL WHERE \"ITEM_ID\"=:id")
        .param("id", itemId)
        .update();
  }

  public void finishItem(long itemId, int count) {
    jdbc.sql(
            "UPDATE \"TB_KOFIA_CLCT_JOB_ITEM\" SET \"STS\"='SUCCESS',\"RECEIVED_CNT\"=:count,\"STORED_CNT\"=:count,\"COMPLETE_DTTM\"=CURRENT_TIMESTAMP WHERE \"ITEM_ID\"=:id")
        .param("id", itemId)
        .param("count", count)
        .update();
  }

  public void failItem(long itemId, String error) {
    jdbc.sql(
            "UPDATE \"TB_KOFIA_CLCT_JOB_ITEM\" SET \"STS\"='FAILED',\"ERROR_MSG\"=:error,\"COMPLETE_DTTM\"=CURRENT_TIMESTAMP WHERE \"ITEM_ID\"=:id")
        .param("id", itemId)
        .param("error", KofiaSupport.trim(error))
        .update();
  }

  public void complete(UUID id) {
    jdbc.sql(
            """
        UPDATE "TB_KOFIA_CLCT_JOB" j SET
          "SUCCESS_ITEM_CNT"=(SELECT count(*) FROM "TB_KOFIA_CLCT_JOB_ITEM" WHERE "JOB_ID"=j."JOB_ID" AND "STS"='SUCCESS'),
          "FAILED_ITEM_CNT"=(SELECT count(*) FROM "TB_KOFIA_CLCT_JOB_ITEM" WHERE "JOB_ID"=j."JOB_ID" AND "STS"='FAILED'),
          "STS"=CASE WHEN EXISTS(SELECT 1 FROM "TB_KOFIA_CLCT_JOB_ITEM" WHERE "JOB_ID"=j."JOB_ID" AND "STS"='FAILED')
            THEN 'COMPLETED_WITH_ERRORS' ELSE 'COMPLETED' END,"COMPLETE_DTTM"=CURRENT_TIMESTAMP WHERE "JOB_ID"=:id
        """)
        .param("id", id)
        .update();
  }

  public void failJob(UUID id, String error) {
    jdbc.sql(
            "UPDATE \"TB_KOFIA_CLCT_JOB\" SET \"STS\"='FAILED',\"ERROR_MSG\"=:error,\"COMPLETE_DTTM\"=CURRENT_TIMESTAMP WHERE \"JOB_ID\"=:id")
        .param("id", id)
        .param("error", KofiaSupport.trim(error))
        .update();
  }

  public int retryFailures(UUID id) {
    int count =
        jdbc.sql(
                "UPDATE \"TB_KOFIA_CLCT_JOB_ITEM\" SET \"STS\"='PENDING',\"ERROR_MSG\"=NULL,\"START_DTTM\"=NULL,\"COMPLETE_DTTM\"=NULL WHERE \"JOB_ID\"=:id AND \"STS\"='FAILED'")
            .param("id", id)
            .update();
    if (count > 0)
      jdbc.sql(
              "UPDATE \"TB_KOFIA_CLCT_JOB\" SET \"STS\"='QUEUED',\"ERROR_MSG\"=NULL,\"COMPLETE_DTTM\"=NULL WHERE \"JOB_ID\"=:id")
          .param("id", id)
          .update();
    return count;
  }

  public JobView job(UUID id) {
    JobView base =
        jdbc.sql("SELECT * FROM \"TB_KOFIA_CLCT_JOB\" WHERE \"JOB_ID\"=:id")
            .param("id", id)
            .query(
                (rs, n) ->
                    new JobView(
                        rs.getObject("JOB_ID", UUID.class),
                        rs.getDate("FROM_DT").toLocalDate(),
                        rs.getDate("TO_DT").toLocalDate(),
                        rs.getString("DATASET_CDS"),
                        rs.getString("STS"),
                        rs.getInt("TOTAL_ITEM_CNT"),
                        rs.getInt("SUCCESS_ITEM_CNT"),
                        rs.getInt("FAILED_ITEM_CNT"),
                        rs.getString("ERROR_MSG"),
                        time(rs.getTimestamp("CRT_DTTM")),
                        time(rs.getTimestamp("START_DTTM")),
                        time(rs.getTimestamp("COMPLETE_DTTM")),
                        List.of()))
            .optional()
            .orElseThrow(() -> new NoSuchElementException("KOFIA 수집 Job을 찾을 수 없습니다: " + id));
    List<ItemView> items =
        jdbc.sql(
                "SELECT * FROM \"TB_KOFIA_CLCT_JOB_ITEM\" WHERE \"JOB_ID\"=:id ORDER BY \"FROM_DT\",\"DATASET_CD\"")
            .param("id", id)
            .query(
                (rs, n) ->
                    new ItemView(
                        rs.getLong("ITEM_ID"),
                        rs.getString("DATASET_CD"),
                        rs.getDate("FROM_DT").toLocalDate(),
                        rs.getDate("TO_DT").toLocalDate(),
                        rs.getString("STS"),
                        rs.getInt("RECEIVED_CNT"),
                        rs.getInt("STORED_CNT"),
                        rs.getString("ERROR_MSG"),
                        time(rs.getTimestamp("START_DTTM")),
                        time(rs.getTimestamp("COMPLETE_DTTM"))))
            .list();
    return new JobView(
        base.jobId(),
        base.from(),
        base.to(),
        base.datasetCodes(),
        base.status(),
        base.totalItemCount(),
        base.successItemCount(),
        base.failedItemCount(),
        base.error(),
        base.createdAt(),
        base.startedAt(),
        base.completedAt(),
        items);
  }

  public List<JobView> jobs(int limit) {
    return jdbc
        .sql("SELECT \"JOB_ID\" FROM \"TB_KOFIA_CLCT_JOB\" ORDER BY \"CRT_DTTM\" DESC LIMIT :limit")
        .param("limit", Math.min(Math.max(limit, 1), 100))
        .query(UUID.class)
        .list()
        .stream()
        .map(this::job)
        .toList();
  }

  private LocalDateTime time(java.sql.Timestamp value) {
    return value == null ? null : value.toLocalDateTime();
  }

  public record PendingItem(long itemId, KofiaDataset dataset, LocalDate from, LocalDate to) {}

  public record ItemView(
      long itemId,
      String dataset,
      LocalDate from,
      LocalDate to,
      String status,
      int receivedCount,
      int storedCount,
      String error,
      LocalDateTime startedAt,
      LocalDateTime completedAt) {}

  public record JobView(
      UUID jobId,
      LocalDate from,
      LocalDate to,
      String datasetCodes,
      String status,
      int totalItemCount,
      int successItemCount,
      int failedItemCount,
      String error,
      LocalDateTime createdAt,
      LocalDateTime startedAt,
      LocalDateTime completedAt,
      List<ItemView> items) {
    public double progressRate() {
      return totalItemCount == 0
          ? 100
          : Math.round((successItemCount + failedItemCount) * 10000.0 / totalItemCount) / 100.0;
    }
  }
}
