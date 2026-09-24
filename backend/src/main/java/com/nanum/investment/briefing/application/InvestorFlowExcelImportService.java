package com.nanum.investment.briefing.application;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class InvestorFlowExcelImportService {
  private static final List<String> EXPECTED_HEADERS =
      List.of(
          "일자",
          "금융투자",
          "보험",
          "투신",
          "사모",
          "은행",
          "기타금융",
          "연기금 등",
          "기타법인",
          "개인",
          "외국인",
          "기타외국인",
          "전체");
  private static final List<String> INVESTOR_CODES =
      List.of(
          "FINANCIAL_INVESTMENT",
          "INSURANCE",
          "INVESTMENT_TRUST",
          "PRIVATE_EQUITY",
          "BANK",
          "OTHER_FINANCE",
          "PENSION_FUND",
          "OTHER_CORPORATION",
          "INDIVIDUAL",
          "FOREIGN",
          "OTHER_FOREIGN",
          "TOTAL");
  private static final List<String> AGGREGATED_HEADERS =
      List.of("일자", "기관 합계", "기타법인", "개인", "외국인 합계", "전체");
  private static final List<String> AGGREGATED_INVESTOR_CODES =
      List.of("INSTITUTION_TOTAL", "OTHER_CORPORATION", "INDIVIDUAL", "FOREIGN_TOTAL", "TOTAL");
  private static final DateTimeFormatter SOURCE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ROOT);

  private final JdbcClient jdbc;
  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactions;
  private final Path importDirectory;

  public InvestorFlowExcelImportService(
      JdbcClient jdbc,
      JdbcTemplate jdbcTemplate,
      TransactionTemplate transactions,
      @Value("${investor-flow.import-dir:/data/investor-flow}") String importDirectory) {
    this.jdbc = jdbc;
    this.jdbcTemplate = jdbcTemplate;
    this.transactions = transactions;
    this.importDirectory = Path.of(importDirectory).toAbsolutePath().normalize();
  }

  public ImportResult importByFilenameToken(String filenameToken) {
    if (filenameToken == null || !filenameToken.matches("[0-9]{8}")) {
      throw new IllegalArgumentException("파일명 날짜는 YYYYMMDD 형식이어야 합니다.");
    }
    if (!Files.isDirectory(importDirectory)) {
      throw new IllegalStateException("투자자 수급 Import 폴더가 없습니다: " + importDirectory);
    }

    List<Path> files = findFiles(filenameToken);
    int completed = 0;
    int failed = 0;
    int skipped = 0;
    int rawRows = 0;
    List<FileOutcome> outcomes = new ArrayList<>();
    for (Path file : files) {
      String relativePath = normalizeRelativePath(file);
      Metadata metadata = metadata(importDirectory, file);
      String hash = sha256(file);
      if (isRegistered(hash)) {
        skipped++;
        outcomes.add(new FileOutcome(relativePath, "SKIPPED", 0, "동일한 파일 해시가 이미 등록되었습니다."));
        continue;
      }
      try {
        ResolvedScope scope = resolveScope(metadata);
        ParsedFile parsed = parse(file);
        transactions.executeWithoutResult(
            status -> saveCompleted(file, relativePath, hash, metadata, scope, parsed));
        completed++;
        rawRows += parsed.values().size();
        outcomes.add(new FileOutcome(relativePath, "COMPLETED", parsed.values().size(), null));
      } catch (RuntimeException exception) {
        transactions.executeWithoutResult(
            status -> saveFailed(file, relativePath, hash, metadata, exception.getMessage()));
        failed++;
        outcomes.add(new FileOutcome(relativePath, "FAILED", 0, exception.getMessage()));
      }
    }
    int normalizedRows = normalize();
    return new ImportResult(
        filenameToken,
        files.size(),
        completed,
        failed,
        skipped,
        rawRows,
        normalizedRows,
        outcomes);
  }

  private List<Path> findFiles(String filenameToken) {
    try (Stream<Path> paths = Files.walk(importDirectory)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().contains(filenameToken))
          .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xlsx"))
          .sorted(Comparator.comparing(this::normalizeRelativePath))
          .toList();
    } catch (IOException exception) {
      throw new IllegalStateException("투자자 수급 Excel 파일 목록을 읽을 수 없습니다.", exception);
    }
  }

  static Metadata metadata(Path root, Path file) {
    Path normalizedRoot = root.toAbsolutePath().normalize();
    Path normalizedFile = file.toAbsolutePath().normalize();
    if (!normalizedFile.startsWith(normalizedRoot)) {
      throw new IllegalArgumentException("Import 폴더 밖의 파일입니다.");
    }
    String filename = normalizedFile.getFileName().toString();
    MetricType metricType =
        filename.contains("거래대금")
            ? MetricType.AMOUNT
            : filename.contains("거래량")
                ? MetricType.VOLUME
                : null;
    TradeType tradeType =
        filename.contains("순매수")
            ? TradeType.NET_BUY
            : filename.contains("매수")
                ? TradeType.BUY
                : filename.contains("매도") ? TradeType.SELL : null;
    if (metricType == null || tradeType == null) {
      throw new IllegalArgumentException("파일명에서 거래구분을 판별할 수 없습니다: " + filename);
    }
    boolean marketScope = normalizedFile.getParent().equals(normalizedRoot);
    String stockName =
        marketScope ? null : normalizedRoot.relativize(normalizedFile).getName(0).toString();
    return new Metadata(
        marketScope ? ScopeType.MARKET : ScopeType.STOCK,
        marketScope ? "ALL" : stockName,
        stockName,
        metricType,
        tradeType);
  }

  private ResolvedScope resolveScope(Metadata metadata) {
    if (metadata.scopeType() == ScopeType.MARKET) {
      return new ResolvedScope("MARKET:ALL", "ALL", null, null);
    }
    List<Map<String, Object>> stocks =
        jdbc.sql(
                """
                SELECT "STK_CD","STK_NM" FROM "TB_STK"
                WHERE "MKT_CD"='KO' AND "STK_NM"=:stockName AND "DEL_YN"='N'
                ORDER BY "STK_ID"
                """)
            .param("stockName", metadata.stockName())
            .query()
            .listOfRows();
    if (stocks.size() != 1) {
      throw new IllegalArgumentException(
          "국내 종목명 매핑 결과가 1건이 아닙니다: " + metadata.stockName() + " (" + stocks.size() + "건)");
    }
    String stockCode = String.valueOf(stocks.get(0).get("STK_CD"));
    return new ResolvedScope("STOCK:" + stockCode, "KO", stockCode, metadata.stockName());
  }

  private ParsedFile parse(Path file) {
    try (InputStream input = Files.newInputStream(file); Workbook workbook = new XSSFWorkbook(input)) {
      if (workbook.getNumberOfSheets() != 1) {
        throw new IllegalArgumentException("Excel 시트가 정확히 1개가 아닙니다.");
      }
      Sheet sheet = workbook.getSheetAt(0);
      Row header = sheet.getRow(0);
      if (header == null) {
        throw new IllegalArgumentException("Excel 데이터가 비어 있습니다.");
      }
      DataFormatter formatter = new DataFormatter(Locale.KOREA);
      List<String> actualHeaders = new ArrayList<>();
      for (int column = 0; column < EXPECTED_HEADERS.size(); column++) {
        actualHeaders.add(formatter.formatCellValue(header.getCell(column)).trim());
      }
      while (!actualHeaders.isEmpty() && actualHeaders.get(actualHeaders.size() - 1).isBlank()) {
        actualHeaders.remove(actualHeaders.size() - 1);
      }
      List<String> investorCodes = investorCodes(actualHeaders);

      List<RawValue> values = new ArrayList<>();
      LocalDate minimumDate = null;
      LocalDate maximumDate = null;
      int dataRows = 0;
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null || formatter.formatCellValue(row.getCell(0)).isBlank()) {
          continue;
        }
        LocalDate date = LocalDate.parse(formatter.formatCellValue(row.getCell(0)).trim(), SOURCE_DATE_FORMAT);
        minimumDate = minimumDate == null || date.isBefore(minimumDate) ? date : minimumDate;
        maximumDate = maximumDate == null || date.isAfter(maximumDate) ? date : maximumDate;
        dataRows++;
        for (int column = 1; column <= investorCodes.size(); column++) {
          values.add(
              new RawValue(
                  date, investorCodes.get(column - 1), numeric(row.getCell(column), formatter)));
        }
      }
      if (dataRows == 0) {
        throw new IllegalArgumentException("Excel 데이터 행이 없습니다.");
      }
      return new ParsedFile(dataRows, minimumDate, maximumDate, values);
    } catch (IOException exception) {
      throw new IllegalStateException("Excel 파일을 읽을 수 없습니다.", exception);
    }
  }

  static List<String> investorCodes(List<String> headers) {
    if (EXPECTED_HEADERS.equals(headers)) {
      return INVESTOR_CODES;
    }
    if (AGGREGATED_HEADERS.equals(headers)) {
      return AGGREGATED_INVESTOR_CODES;
    }
    int mismatch = 0;
    while (mismatch < headers.size()
        && mismatch < EXPECTED_HEADERS.size()
        && EXPECTED_HEADERS.get(mismatch).equals(headers.get(mismatch))) {
      mismatch++;
    }
    String actual = mismatch < headers.size() ? headers.get(mismatch) : "";
    throw new IllegalArgumentException("Excel 헤더가 다릅니다: " + (mismatch + 1) + "열 " + actual);
  }

  private static BigDecimal numeric(Cell cell, DataFormatter formatter) {
    String value = formatter.formatCellValue(cell).trim().replace(",", "");
    if (value.isBlank()) {
      throw new IllegalArgumentException("필수 수치 셀이 비어 있습니다.");
    }
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("수치 셀 형식이 올바르지 않습니다: " + value, exception);
    }
  }

  private void saveCompleted(
      Path file,
      String relativePath,
      String hash,
      Metadata metadata,
      ResolvedScope scope,
      ParsedFile parsed) {
    long fileId =
        jdbc.sql(
                """
                INSERT INTO "TB_INV_FLOW_FILE"(
                  "SOURCE_FILE_NM","SOURCE_REL_PATH","FILE_HASH","SCOPE_TP","SCOPE_KEY",
                  "MKT_CD","STK_CD","STK_NM","METRIC_TP","TRADE_TP","UNIT_MULT",
                  "IMPORT_STATUS","ROW_CNT","MIN_BASE_DT","MAX_BASE_DT")
                VALUES(:filename,:relativePath,:hash,:scopeType,:scopeKey,:marketCode,:stockCode,
                  :stockName,:metricType,:tradeType,1,'COMPLETED',:rowCount,:minimumDate,:maximumDate)
                RETURNING "INV_FLOW_FILE_ID"
                """)
            .param("filename", file.getFileName().toString())
            .param("relativePath", relativePath)
            .param("hash", hash)
            .param("scopeType", metadata.scopeType().name())
            .param("scopeKey", scope.scopeKey())
            .param("marketCode", scope.marketCode())
            .param("stockCode", scope.stockCode())
            .param("stockName", scope.stockName())
            .param("metricType", metadata.metricType().name())
            .param("tradeType", metadata.tradeType().name())
            .param("rowCount", parsed.dataRows())
            .param("minimumDate", parsed.minimumDate())
            .param("maximumDate", parsed.maximumDate())
            .query(Long.class)
            .single();

    jdbcTemplate.batchUpdate(
        """
        INSERT INTO "TB_INV_FLOW_RAW_ROW"("INV_FLOW_FILE_ID","BASE_DT","INVESTOR_TP","RAW_VAL")
        VALUES(?,?,?,?)
        """,
        parsed.values().stream()
            .map(value -> new Object[] {fileId, value.baseDate(), value.investorType(), value.value()})
            .toList());
  }

  private void saveFailed(
      Path file, String relativePath, String hash, Metadata metadata, String errorMessage) {
    jdbc.sql(
            """
            INSERT INTO "TB_INV_FLOW_FILE"(
              "SOURCE_FILE_NM","SOURCE_REL_PATH","FILE_HASH","SCOPE_TP","SCOPE_KEY","MKT_CD",
              "STK_NM","METRIC_TP","TRADE_TP","IMPORT_STATUS","ERROR_MSG")
            VALUES(:filename,:relativePath,:hash,:scopeType,:scopeKey,'UNKNOWN',:stockName,
              :metricType,:tradeType,'FAILED',:errorMessage)
            ON CONFLICT ("FILE_HASH") DO UPDATE SET
              "IMPORT_STATUS"='FAILED',"ERROR_MSG"=EXCLUDED."ERROR_MSG","MOD_DTTM"=CURRENT_TIMESTAMP
            WHERE "TB_INV_FLOW_FILE"."IMPORT_STATUS"<>'COMPLETED'
            """)
        .param("filename", file.getFileName().toString())
        .param("relativePath", relativePath)
        .param("hash", hash)
        .param("scopeType", metadata.scopeType().name())
        .param("scopeKey", metadata.scopeType() == ScopeType.MARKET ? "MARKET:ALL" : "STOCK:" + metadata.scopeSubject())
        .param("stockName", metadata.stockName())
        .param("metricType", metadata.metricType().name())
        .param("tradeType", metadata.tradeType().name())
        .param("errorMessage", Optional.ofNullable(errorMessage).orElse("알 수 없는 오류"))
        .update();
  }

  private boolean isRegistered(String hash) {
    return jdbc.sql(
            "SELECT EXISTS(SELECT 1 FROM \"TB_INV_FLOW_FILE\" WHERE \"FILE_HASH\"=:hash)")
        .param("hash", hash)
        .query(Boolean.class)
        .single();
  }

  private int normalize() {
    return jdbc.sql(
            """
            WITH latest_files AS (
              SELECT f.*,
                     row_number() OVER (PARTITION BY f."SCOPE_TP",f."SCOPE_KEY",f."METRIC_TP",
                       f."TRADE_TP" ORDER BY f."INV_FLOW_FILE_ID" DESC) AS rn
              FROM "TB_INV_FLOW_FILE" f
              WHERE f."IMPORT_STATUS"='COMPLETED'
            ), source AS (
              SELECT f.*,r."BASE_DT",r."INVESTOR_TP",r."RAW_VAL"*f."UNIT_MULT" AS value
              FROM latest_files f
              JOIN "TB_INV_FLOW_RAW_ROW" r ON r."INV_FLOW_FILE_ID"=f."INV_FLOW_FILE_ID"
              WHERE f.rn=1
            ), pivoted AS (
              SELECT "SCOPE_TP","SCOPE_KEY",max("MKT_CD") AS market_code,max("STK_CD") AS stock_code,
                     max("STK_NM") AS stock_name,"BASE_DT","INVESTOR_TP",
                     max(value) FILTER (WHERE "METRIC_TP"='VOLUME' AND "TRADE_TP"='SELL') AS sell_qty,
                     max(value) FILTER (WHERE "METRIC_TP"='VOLUME' AND "TRADE_TP"='BUY') AS buy_qty,
                     max(value) FILTER (WHERE "METRIC_TP"='VOLUME' AND "TRADE_TP"='NET_BUY') AS net_qty,
                     max(value) FILTER (WHERE "METRIC_TP"='AMOUNT' AND "TRADE_TP"='SELL') AS sell_amt,
                     max(value) FILTER (WHERE "METRIC_TP"='AMOUNT' AND "TRADE_TP"='BUY') AS buy_amt,
                     max(value) FILTER (WHERE "METRIC_TP"='AMOUNT' AND "TRADE_TP"='NET_BUY') AS net_amt,
                     md5(string_agg(DISTINCT "FILE_HASH",',' ORDER BY "FILE_HASH")) AS source_hash
              FROM source
              GROUP BY "SCOPE_TP","SCOPE_KEY","BASE_DT","INVESTOR_TP"
            ), deleted AS (
              DELETE FROM "TB_INV_FLOW_DAY" d
              WHERE EXISTS (
                SELECT 1 FROM pivoted p
                WHERE p."SCOPE_TP"=d."SCOPE_TP" AND p."SCOPE_KEY"=d."SCOPE_KEY"
                  AND p."BASE_DT"=d."BASE_DT")
                AND NOT EXISTS (
                  SELECT 1 FROM pivoted p
                  WHERE p."SCOPE_TP"=d."SCOPE_TP" AND p."SCOPE_KEY"=d."SCOPE_KEY"
                    AND p."BASE_DT"=d."BASE_DT" AND p."INVESTOR_TP"=d."INVESTOR_TP")
              RETURNING 1
            )
            INSERT INTO "TB_INV_FLOW_DAY"(
              "SCOPE_TP","SCOPE_KEY","MKT_CD","STK_CD","STK_NM","BASE_DT","INVESTOR_TP",
              "SELL_QTY","BUY_QTY","NET_BUY_QTY","SELL_AMT","BUY_AMT","NET_BUY_AMT",
              "NET_QTY_DERIVED_YN","NET_AMT_DERIVED_YN","SOURCE_DATA_HASH")
            SELECT "SCOPE_TP","SCOPE_KEY",market_code,stock_code,stock_name,"BASE_DT","INVESTOR_TP",
                   sell_qty,buy_qty,COALESCE(net_qty,buy_qty-sell_qty),sell_amt,buy_amt,
                   COALESCE(net_amt,buy_amt-sell_amt),
                   CASE WHEN net_qty IS NULL AND buy_qty IS NOT NULL AND sell_qty IS NOT NULL THEN 'Y' ELSE 'N' END,
                   CASE WHEN net_amt IS NULL AND buy_amt IS NOT NULL AND sell_amt IS NOT NULL THEN 'Y' ELSE 'N' END,
                   source_hash
            FROM pivoted
            ON CONFLICT ("SCOPE_TP","SCOPE_KEY","BASE_DT","INVESTOR_TP") DO UPDATE SET
              "MKT_CD"=EXCLUDED."MKT_CD","STK_CD"=EXCLUDED."STK_CD","STK_NM"=EXCLUDED."STK_NM",
              "SELL_QTY"=EXCLUDED."SELL_QTY","BUY_QTY"=EXCLUDED."BUY_QTY",
              "NET_BUY_QTY"=EXCLUDED."NET_BUY_QTY","SELL_AMT"=EXCLUDED."SELL_AMT",
              "BUY_AMT"=EXCLUDED."BUY_AMT","NET_BUY_AMT"=EXCLUDED."NET_BUY_AMT",
              "NET_QTY_DERIVED_YN"=EXCLUDED."NET_QTY_DERIVED_YN",
              "NET_AMT_DERIVED_YN"=EXCLUDED."NET_AMT_DERIVED_YN",
              "SOURCE_DATA_HASH"=EXCLUDED."SOURCE_DATA_HASH","MOD_DTTM"=CURRENT_TIMESTAMP
            """)
        .update();
  }

  private String normalizeRelativePath(Path file) {
    return importDirectory.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
  }

  private static String sha256(Path file) {
    try (InputStream input = Files.newInputStream(file)) {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[8192];
      int length;
      while ((length = input.read(buffer)) >= 0) {
        digest.update(buffer, 0, length);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (IOException | NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Excel 파일 해시를 계산할 수 없습니다.", exception);
    }
  }

  enum ScopeType { MARKET, STOCK }

  enum MetricType { AMOUNT, VOLUME }

  enum TradeType { SELL, BUY, NET_BUY }

  record Metadata(
      ScopeType scopeType,
      String scopeSubject,
      String stockName,
      MetricType metricType,
      TradeType tradeType) {}

  private record ResolvedScope(String scopeKey, String marketCode, String stockCode, String stockName) {}

  private record RawValue(LocalDate baseDate, String investorType, BigDecimal value) {}

  private record ParsedFile(
      int dataRows, LocalDate minimumDate, LocalDate maximumDate, List<RawValue> values) {}

  public record FileOutcome(String relativePath, String status, int rawRows, String reason) {}

  public record ImportResult(
      String filenameToken,
      int matchedFiles,
      int completedFiles,
      int failedFiles,
      int skippedFiles,
      int insertedRawRows,
      int normalizedRows,
      List<FileOutcome> files) {}
}
