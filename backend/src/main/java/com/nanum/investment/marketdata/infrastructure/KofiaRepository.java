package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.marketdata.domain.KofiaDataset;
import com.nanum.investment.marketdata.domain.KofiaEquityMarketStatistic;
import com.nanum.investment.marketdata.domain.KofiaFinalQuotedYield;
import com.nanum.investment.marketdata.domain.KofiaOtcBondInvestorTrade;
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
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  public KofiaRepository(
      JdbcClient jdbc, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public int save(
      UUID jobId,
      KofiaDataset dataset,
      LocalDate from,
      LocalDate to,
      JsonNode raw,
      Map<String, Object> requestParameters,
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
          INSERT INTO "TB_KOFIA_DATA_ROW"("DATASET_CD","BASE_DT","ROW_KEY","PAYLOAD","RAW_HASH",
            "ROW_TYPE","ENTITY_CD","ENTITY_NM","REQ_PARAMS")
          VALUES(:dataset,:day,:key,CAST(:payload AS jsonb),:hash,:rowType,:entityCode,:entityName,
            CAST(:requestParams AS jsonb))
          ON CONFLICT("DATASET_CD","BASE_DT","ROW_KEY") DO UPDATE SET
            "PAYLOAD"=EXCLUDED."PAYLOAD","RAW_HASH"=EXCLUDED."RAW_HASH",
            "ROW_TYPE"=EXCLUDED."ROW_TYPE","ENTITY_CD"=EXCLUDED."ENTITY_CD",
            "ENTITY_NM"=EXCLUDED."ENTITY_NM","REQ_PARAMS"=EXCLUDED."REQ_PARAMS",
            "LAST_COLLECT_DTTM"=CURRENT_TIMESTAMP
          """)
          .param("dataset", dataset.name())
          .param("day", row.baseDate())
          .param("key", row.rowKey())
          .param("payload", row.payload().toString())
          .param("hash", rowHash)
          .param("rowType", row.rowType())
          .param("entityCode", row.entityCode())
          .param("entityName", row.entityName())
          .param("requestParams", json(requestParameters))
          .update();
      if (dataset == KofiaDataset.CREDIT_BALANCE_TREND) saveCreditBalance(row, rowHash);
      if (dataset == KofiaDataset.SECURITIES_LENDING_TREND) saveSecuritiesLending(row, rowHash);
      if (dataset == KofiaDataset.MARKET_FUNDS_TREND) saveMarketFunds(row, rowHash);
      if (dataset == KofiaDataset.OTC_INVESTOR_TRADING)
        saveOtcBondInvestorTrades(row, from, to, requestParameters, rowHash);
      if (dataset == KofiaDataset.FINAL_QUOTED_YIELD) saveFinalQuotedYield(row, rowHash);
      if (dataset == KofiaDataset.KOSPI_MARKET || dataset == KofiaDataset.KOSDAQ_MARKET)
        saveEquityMarketStatistic(dataset, row, requestParameters, rowHash);
    }
    return rows.size();
  }

  private void saveFinalQuotedYield(KofiaClient.KofiaRow row, String hash) {
    KofiaFinalQuotedYield value = KofiaFinalQuotedYield.from(row.baseDate(), row.payload());
    jdbc.sql(
            """
        INSERT INTO "TB_KOFIA_FINAL_QUOTE_YLD"(
          "BASE_DT","INSTRUMENT_NM","REMAIN_TERM_NM","MORNING_YLD_RT","AFTERNOON_YLD_RT",
          "DAY_CHG_PT","PREV_YLD_RT","YEAR_HIGH_YLD_RT","YEAR_LOW_YLD_RT","RAW_HASH")
        VALUES(:day,:instrument,:remaining,:morning,:afternoon,:change,:previous,:yearHigh,
          :yearLow,:hash)
        ON CONFLICT("BASE_DT","INSTRUMENT_NM","REMAIN_TERM_NM") DO UPDATE SET
          "MORNING_YLD_RT"=EXCLUDED."MORNING_YLD_RT",
          "AFTERNOON_YLD_RT"=EXCLUDED."AFTERNOON_YLD_RT",
          "DAY_CHG_PT"=EXCLUDED."DAY_CHG_PT","PREV_YLD_RT"=EXCLUDED."PREV_YLD_RT",
          "YEAR_HIGH_YLD_RT"=EXCLUDED."YEAR_HIGH_YLD_RT",
          "YEAR_LOW_YLD_RT"=EXCLUDED."YEAR_LOW_YLD_RT","RAW_HASH"=EXCLUDED."RAW_HASH",
          "DATA_STS"='FRESH',"COLLECT_DTTM"=CURRENT_TIMESTAMP,
          "UPD_DTTM"=CASE WHEN "TB_KOFIA_FINAL_QUOTE_YLD"."RAW_HASH"<>EXCLUDED."RAW_HASH"
            THEN CURRENT_TIMESTAMP ELSE "TB_KOFIA_FINAL_QUOTE_YLD"."UPD_DTTM" END
        """)
        .param("day", value.baseDate())
        .param("instrument", value.instrumentName())
        .param("remaining", value.remainingTermName())
        .param("morning", value.morningYieldRate())
        .param("afternoon", value.afternoonYieldRate())
        .param("change", value.dayChangePoint())
        .param("previous", value.previousYieldRate())
        .param("yearHigh", value.yearHighYieldRate())
        .param("yearLow", value.yearLowYieldRate())
        .param("hash", hash)
        .update();
  }

  private void saveEquityMarketStatistic(
      KofiaDataset dataset,
      KofiaClient.KofiaRow row,
      Map<String, Object> requestParameters,
      String hash) {
    KofiaEquityMarketStatistic value =
        KofiaEquityMarketStatistic.from(dataset, row.payload(), requestParameters);
    jdbc.sql(
            """
        INSERT INTO "TB_KOFIA_EQUITY_MKT_DAY"(
          "MKT_CD","BASE_DT","INDEX_VAL","TRD_QTY","TRD_AMT","MKT_CAP","FOREIGN_MKT_CAP",
          "FOREIGN_MKT_CAP_RT","QTY_UNIT_MULTIPLIER","AMT_UNIT_MULTIPLIER","RAW_HASH")
        VALUES(:market,:day,:indexValue,:quantity,:amount,:marketCap,:foreignMarketCap,
          :foreignRate,:quantityUnit,:amountUnit,:hash)
        ON CONFLICT("MKT_CD","BASE_DT") DO UPDATE SET
          "INDEX_VAL"=EXCLUDED."INDEX_VAL","TRD_QTY"=EXCLUDED."TRD_QTY",
          "TRD_AMT"=EXCLUDED."TRD_AMT","MKT_CAP"=EXCLUDED."MKT_CAP",
          "FOREIGN_MKT_CAP"=EXCLUDED."FOREIGN_MKT_CAP",
          "FOREIGN_MKT_CAP_RT"=EXCLUDED."FOREIGN_MKT_CAP_RT",
          "QTY_UNIT_MULTIPLIER"=EXCLUDED."QTY_UNIT_MULTIPLIER",
          "AMT_UNIT_MULTIPLIER"=EXCLUDED."AMT_UNIT_MULTIPLIER",
          "RAW_HASH"=EXCLUDED."RAW_HASH","DATA_STS"='FRESH',"COLLECT_DTTM"=CURRENT_TIMESTAMP,
          "UPD_DTTM"=CASE WHEN "TB_KOFIA_EQUITY_MKT_DAY"."RAW_HASH"<>EXCLUDED."RAW_HASH"
            THEN CURRENT_TIMESTAMP ELSE "TB_KOFIA_EQUITY_MKT_DAY"."UPD_DTTM" END
        """)
        .param("market", value.marketCode())
        .param("day", row.baseDate())
        .param("indexValue", value.indexValue())
        .param("quantity", value.tradingQuantity())
        .param("amount", value.tradingAmount())
        .param("marketCap", value.marketCapitalization())
        .param("foreignMarketCap", value.foreignMarketCapitalization())
        .param("foreignRate", value.foreignMarketCapitalizationRate())
        .param("quantityUnit", value.quantityUnitMultiplier())
        .param("amountUnit", value.amountUnitMultiplier())
        .param("hash", hash)
        .update();
  }

  private void saveOtcBondInvestorTrades(
      KofiaClient.KofiaRow row,
      LocalDate from,
      LocalDate to,
      Map<String, Object> requestParameters,
      String hash) {
    for (KofiaOtcBondInvestorTrade value :
        KofiaOtcBondInvestorTrade.from(row.payload(), from, to, requestParameters)) {
      jdbc.sql(
              """
          INSERT INTO "TB_KOFIA_BND_OTC_INV_TRD"(
            "DATA_PRD_CD","FROM_DT","TO_DT","VALUE_TYP_CD","TRADE_TYP_NM","BOND_TYP_NM",
            "INVESTOR_TYP_CD","INVESTOR_TYP_NM","REMAIN_FROM_MON","REMAIN_TO_MON",
            "UNIT_MULTIPLIER","TRD_VALUE","RAW_HASH")
          VALUES(:period,:from,:to,:valueType,:tradeType,:bondType,:investorType,:investorName,
            :remainFrom,:remainTo,:unitMultiplier,:value,:hash)
          ON CONFLICT("DATA_PRD_CD","FROM_DT","TO_DT","VALUE_TYP_CD","TRADE_TYP_NM",
            "BOND_TYP_NM","INVESTOR_TYP_CD","REMAIN_FROM_MON","REMAIN_TO_MON") DO UPDATE SET
            "INVESTOR_TYP_NM"=EXCLUDED."INVESTOR_TYP_NM",
            "UNIT_MULTIPLIER"=EXCLUDED."UNIT_MULTIPLIER","TRD_VALUE"=EXCLUDED."TRD_VALUE",
            "RAW_HASH"=EXCLUDED."RAW_HASH","DATA_STS"='FRESH',"COLLECT_DTTM"=CURRENT_TIMESTAMP,
            "UPD_DTTM"=CASE WHEN "TB_KOFIA_BND_OTC_INV_TRD"."RAW_HASH"<>EXCLUDED."RAW_HASH"
              THEN CURRENT_TIMESTAMP ELSE "TB_KOFIA_BND_OTC_INV_TRD"."UPD_DTTM" END
          """)
          .param("period", value.dataPeriodCode())
          .param("from", value.from())
          .param("to", value.to())
          .param("valueType", value.valueTypeCode())
          .param("tradeType", value.tradeTypeName())
          .param("bondType", value.bondTypeName())
          .param("investorType", value.investorTypeCode())
          .param("investorName", value.investorTypeName())
          .param("remainFrom", value.remainingFromMonths())
          .param("remainTo", value.remainingToMonths())
          .param("unitMultiplier", value.unitMultiplier())
          .param("value", value.value())
          .param("hash", hash)
          .update();
    }
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

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception error) {
      throw new IllegalStateException("KOFIA 요청조건 JSON 변환에 실패했습니다.", error);
    }
  }

  public List<Map<String, Object>> dataRows(
      KofiaDataset dataset, LocalDate from, LocalDate to, int limit) {
    return jdbc.sql(
            """
        SELECT "DATASET_CD" dataset_code,"BASE_DT" base_date,"ROW_KEY" row_key,
          "ROW_TYPE" row_type,"ENTITY_CD" entity_code,"ENTITY_NM" entity_name,
          "REQ_PARAMS" request_parameters,"PAYLOAD" payload,"RAW_HASH" raw_hash,
          "FIRST_COLLECT_DTTM" first_collected_at,"LAST_COLLECT_DTTM" last_collected_at
        FROM "TB_KOFIA_DATA_ROW"
        WHERE "DATASET_CD"=:dataset AND "BASE_DT" BETWEEN :from AND :to
        ORDER BY "BASE_DT" DESC,"ROW_KEY" LIMIT :limit
        """)
        .param("dataset", dataset.name())
        .param("from", from)
        .param("to", to)
        .param("limit", Math.min(Math.max(limit, 1), 10000))
        .query()
        .listOfRows();
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

  public List<Map<String, Object>> otcBondInvestorTrades(LocalDate from, LocalDate to, int limit) {
    return jdbc.sql(
            """
        SELECT "DATA_PRD_CD" data_period_code,"FROM_DT" from_date,"TO_DT" to_date,
          "VALUE_TYP_CD" value_type_code,"TRADE_TYP_NM" trade_type_name,
          "BOND_TYP_NM" bond_type_name,"INVESTOR_TYP_CD" investor_type_code,
          "INVESTOR_TYP_NM" investor_type_name,"REMAIN_FROM_MON" remaining_from_months,
          "REMAIN_TO_MON" remaining_to_months,"UNIT_MULTIPLIER" unit_multiplier,
          "TRD_VALUE" trade_value,"RAW_HASH" raw_hash,"DATA_STS" data_status,
          "COLLECT_DTTM" collected_at,"UPD_DTTM" updated_at
        FROM "TB_KOFIA_BND_OTC_INV_TRD"
        WHERE "TO_DT" BETWEEN :from AND :to
        ORDER BY "TO_DT" DESC,"TRADE_TYP_NM","BOND_TYP_NM","INVESTOR_TYP_CD"
        LIMIT :limit
        """)
        .param("from", from)
        .param("to", to)
        .param("limit", Math.min(Math.max(limit, 1), 10000))
        .query()
        .listOfRows();
  }

  public List<Map<String, Object>> finalQuotedYields(LocalDate from, LocalDate to, int limit) {
    return jdbc.sql(
            """
        SELECT "BASE_DT" base_date,"INSTRUMENT_NM" instrument_name,
          "REMAIN_TERM_NM" remaining_term_name,"MORNING_YLD_RT" morning_yield_rate,
          "AFTERNOON_YLD_RT" afternoon_yield_rate,"DAY_CHG_PT" day_change_point,
          "PREV_YLD_RT" previous_yield_rate,"YEAR_HIGH_YLD_RT" year_high_yield_rate,
          "YEAR_LOW_YLD_RT" year_low_yield_rate,"RAW_HASH" raw_hash,"DATA_STS" data_status,
          "COLLECT_DTTM" collected_at,"UPD_DTTM" updated_at
        FROM "TB_KOFIA_FINAL_QUOTE_YLD" WHERE "BASE_DT" BETWEEN :from AND :to
        ORDER BY "BASE_DT" DESC,"INSTRUMENT_NM","REMAIN_TERM_NM" LIMIT :limit
        """)
        .param("from", from)
        .param("to", to)
        .param("limit", Math.min(Math.max(limit, 1), 10000))
        .query()
        .listOfRows();
  }

  public List<Map<String, Object>> equityMarketStatistics(
      String marketCode, LocalDate from, LocalDate to, int limit) {
    return jdbc.sql(
            """
        SELECT "MKT_CD" market_code,"BASE_DT" base_date,"INDEX_VAL" index_value,
          "TRD_QTY" trading_quantity,"TRD_AMT" trading_amount,"MKT_CAP" market_capitalization,
          "FOREIGN_MKT_CAP" foreign_market_capitalization,
          "FOREIGN_MKT_CAP_RT" foreign_market_capitalization_rate,
          "QTY_UNIT_MULTIPLIER" quantity_unit_multiplier,
          "AMT_UNIT_MULTIPLIER" amount_unit_multiplier,"RAW_HASH" raw_hash,
          "DATA_STS" data_status,"COLLECT_DTTM" collected_at,"UPD_DTTM" updated_at
        FROM "TB_KOFIA_EQUITY_MKT_DAY"
        WHERE "MKT_CD"=:market AND "BASE_DT" BETWEEN :from AND :to
        ORDER BY "BASE_DT" DESC LIMIT :limit
        """)
        .param("market", marketCode)
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
    for (KofiaDataset dataset : datasets) itemCount += jobRanges(dataset, from, to).size();
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
      for (DateRange range : jobRanges(dataset, from, to)) {
        jdbc.sql(
                """
            INSERT INTO "TB_KOFIA_CLCT_JOB_ITEM"("JOB_ID","DATASET_CD","FROM_DT","TO_DT")
            VALUES(:job,:dataset,:from,:to)
            """)
            .param("job", id)
            .param("dataset", dataset.name())
            .param("from", range.from())
            .param("to", range.to())
            .update();
      }
    }
  }

  private List<DateRange> jobRanges(KofiaDataset dataset, LocalDate from, LocalDate to) {
    List<DateRange> ranges = new java.util.ArrayList<>();
    if (dataset.requiresSingleDateRequest()) {
      List<LocalDate> tradingDates =
          jdbc.sql(
                  """
              SELECT DISTINCT "BASE_DT" FROM "TB_KOFIA_DATA_ROW"
              WHERE "DATASET_CD"='KOSPI_MARKET' AND "BASE_DT" BETWEEN :from AND :to
              ORDER BY "BASE_DT"
              """)
              .param("from", from)
              .param("to", to)
              .query(LocalDate.class)
              .list();
      if (!tradingDates.isEmpty()) {
        tradingDates.forEach(day -> ranges.add(new DateRange(day, day)));
        return ranges;
      }
    }
    int days = dataset.requiresSingleDateRequest() ? 1 : 90;
    for (LocalDate cursor = from; !cursor.isAfter(to); cursor = cursor.plusDays(days)) {
      if (dataset.requiresSingleDateRequest()
          && (cursor.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
              || cursor.getDayOfWeek() == java.time.DayOfWeek.SUNDAY)) continue;
      LocalDate chunkTo = cursor.plusDays(days - 1L).isAfter(to) ? to : cursor.plusDays(days - 1L);
      ranges.add(new DateRange(cursor, chunkTo));
    }
    return ranges;
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

  private record DateRange(LocalDate from, LocalDate to) {}

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
