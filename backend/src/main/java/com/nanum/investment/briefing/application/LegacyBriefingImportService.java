package com.nanum.investment.briefing.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacyBriefingImportService {
  private static final Path INDEX_FILE = Path.of("docs", "briefing_recovery_index.json");
  private static final Path RECOVERED_FILE = Path.of("docs", "briefing_recovered_selected.json");
  private static final Pattern RISK_PATTERN =
      Pattern.compile(
          "(?:시장\\s*)?위험지수(?:는|는\\s*|\\s*[:：-]){0,2}[^0-9]{0,30}(\\d{1,3})(?:\\s*점|\\s*/\\s*100)?");
  private static final Pattern CASH_PATTERN =
      Pattern.compile(
          "(?:현금(?:\\s*투입)?|평소\\s*금액)[^%\\n]{0,40}?(\\d{1,3})(?:\\s*[~～-]\\s*(\\d{1,3}))?\\s*%");

  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;

  public LegacyBriefingImportService(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public ImportResult importWorkspaceFiles() {
    try {
      return importDocuments(
          objectMapper.readTree(Files.readString(INDEX_FILE, StandardCharsets.UTF_8)),
          objectMapper.readTree(Files.readString(RECOVERED_FILE, StandardCharsets.UTF_8)));
    } catch (IOException exception) {
      throw new IllegalStateException(
          "LEGACY 브리핑 복원 파일을 읽을 수 없습니다: " + exception.getMessage(), exception);
    }
  }

  @Transactional
  public ImportResult importDocuments(JsonNode index, JsonNode recovered) {
    if (index == null || !index.isArray())
      throw new IllegalArgumentException("복원 인덱스는 JSON 배열이어야 합니다.");
    if (recovered == null || !recovered.isArray())
      throw new IllegalArgumentException("복원 원문은 JSON 배열이어야 합니다.");
    Map<String, JsonNode> texts = new LinkedHashMap<>();
    recovered.forEach(node -> texts.put(text(node, "message_id"), node));
    UUID batchId = UUID.randomUUID();
    int recoveredCount = 0, incompleteCount = 0, missingCount = 0, reviewRequired = 0;
    for (JsonNode row : index) {
      String messageId = text(row, "selected_message_id");
      JsonNode source = texts.get(messageId);
      String original = source == null ? null : nullableText(source, "text");
      String status = recoveryStatus(text(row, "status"), original);
      if ("RECOVERED".equals(status)) recoveredCount++;
      else if ("INCOMPLETE".equals(status)) incompleteCount++;
      else missingCount++;
      Long id = upsertLegacy(row, source, original, status, batchId);
      if (original != null && extract(id, original)) reviewRequired++;
    }
    return new ImportResult(
        batchId, index.size(), recoveredCount, incompleteCount, missingCount, reviewRequired);
  }

  private Long upsertLegacy(
      JsonNode row, JsonNode source, String original, String status, UUID batchId) {
    LocalDate recoveryDate = LocalDate.parse(text(row, "recovery_date"));
    String displayed = nullableText(row, "display_basis_date");
    LocalDate marketBaseDate = displayed == null ? null : LocalDate.parse(displayed);
    String generatedAt =
        source == null ? nullableText(row, "generated_at") : nullableText(source, "generated_at");
    OffsetDateTime generated = generatedAt == null ? null : OffsetDateTime.parse(generatedAt);
    String briefingType =
        briefingType(
            source == null
                ? nullableText(row, "briefing_type")
                : nullableText(source, "briefing_type"));
    JsonNode metadata = row.deepCopy();
    return jdbc.sql(
            """
            INSERT INTO "TB_LEGACY_BRF"("GENERATED_DTTM","MARKET_BASE_DT","RECOVERY_DT","BRF_TP","SOURCE_TP",
             "CONVERSATION_TITLE","CONVERSATION_ID","MESSAGE_ID","SELECTED_MESSAGE_ID","ORIGINAL_TEXT","ORIGINAL_HASH",
             "RECOVERY_STS","ISSUE_TXT","CANDIDATE_CNT","SOURCE_META_JSON","IMPORT_BATCH_ID")
            VALUES(:generated,:baseDate,:recoveryDate,:type,'CHATGPT_CONVERSATION',:title,:conversationId,:messageId,
             :selectedMessageId,:original,:hash,:status,:issue,:candidateCount,CAST(:metadata AS jsonb),:batchId)
            ON CONFLICT("RECOVERY_DT","SOURCE_TP") DO UPDATE SET
             "GENERATED_DTTM"=EXCLUDED."GENERATED_DTTM","MARKET_BASE_DT"=EXCLUDED."MARKET_BASE_DT","BRF_TP"=EXCLUDED."BRF_TP",
             "CONVERSATION_TITLE"=EXCLUDED."CONVERSATION_TITLE","CONVERSATION_ID"=EXCLUDED."CONVERSATION_ID",
             "MESSAGE_ID"=EXCLUDED."MESSAGE_ID","SELECTED_MESSAGE_ID"=EXCLUDED."SELECTED_MESSAGE_ID",
             "ORIGINAL_TEXT"=EXCLUDED."ORIGINAL_TEXT","ORIGINAL_HASH"=EXCLUDED."ORIGINAL_HASH","RECOVERY_STS"=EXCLUDED."RECOVERY_STS",
             "ISSUE_TXT"=EXCLUDED."ISSUE_TXT","CANDIDATE_CNT"=EXCLUDED."CANDIDATE_CNT","SOURCE_META_JSON"=EXCLUDED."SOURCE_META_JSON",
             "IMPORT_BATCH_ID"=EXCLUDED."IMPORT_BATCH_ID","UPD_DTTM"=CURRENT_TIMESTAMP
            RETURNING "LEGACY_BRF_ID"
            """)
        .param("generated", generated)
        .param("baseDate", marketBaseDate)
        .param("recoveryDate", recoveryDate)
        .param("type", briefingType)
        .param(
            "title",
            source == null
                ? nullableText(row, "conversation_title")
                : nullableText(source, "conversation_title"))
        .param(
            "conversationId",
            source == null
                ? nullableText(row, "conversation_id")
                : nullableText(source, "conversation_id"))
        .param(
            "messageId",
            source == null ? nullableText(row, "message_id") : nullableText(source, "message_id"))
        .param("selectedMessageId", nullableText(row, "selected_message_id"))
        .param("original", original)
        .param("hash", original == null ? null : sha256(original))
        .param("status", status)
        .param("issue", nullableText(row, "issue"))
        .param("candidateCount", row.path("candidate_count").asInt(0))
        .param("metadata", metadata.toString())
        .param("batchId", batchId)
        .query(Long.class)
        .single();
  }

  private boolean extract(Long legacyId, String original) {
    List<Integer> candidates = riskCandidates(original);
    Integer risk =
        candidates.size() == 1
            ? candidates.getFirst()
            : candidates.isEmpty() ? null : candidates.getLast();
    String phase = phaseCode(original);
    Signal regular = regularBuySignal(original);
    Signal additional = additionalBuySignal(original);
    BigDecimal cash = cashRate(original);
    boolean review = candidates.size() > 1;
    int populated =
        (risk == null ? 0 : 1)
            + (phase == null ? 0 : 1)
            + (regular.code() == null ? 0 : 1)
            + (additional.code() == null ? 0 : 1)
            + (cash == null ? 0 : 1);
    BigDecimal confidence =
        BigDecimal.valueOf(review ? Math.min(70, populated * 18L) : Math.min(95, populated * 20L));
    Map<String, Object> extracted = new LinkedHashMap<>();
    extracted.put("riskScoreCandidates", candidates);
    extracted.put("extractionPolicy", "explicit-text-only");
    extracted.put("ambiguousRiskScore", review);
    String json = writeJson(extracted);
    jdbc.sql(
            """
            INSERT INTO "TB_LEGACY_BRF_EXTRACT"("LEGACY_BRF_ID","EXTRACT_VER","MKT_RISK_SCR","MKT_PHASE_RAW","MKT_PHASE_CD",
             "REG_BUY_SIG_RAW","REG_BUY_SIG_CD","ADD_BUY_SIG_RAW","ADD_BUY_SIG_CD","CASH_INPUT_RT","EXTRACTED_JSON",
             "EXTRACT_CONF_RT","REVIEW_STS","REVIEW_NOTE")
            VALUES(:id,1,:risk,:phaseRaw,:phase,:regularRaw,:regular,:additionalRaw,:additional,:cash,CAST(:json AS jsonb),
             :confidence,:reviewStatus,:reviewNote)
            ON CONFLICT("LEGACY_BRF_ID","EXTRACT_VER") DO UPDATE SET
             "MKT_RISK_SCR"=EXCLUDED."MKT_RISK_SCR","MKT_PHASE_RAW"=EXCLUDED."MKT_PHASE_RAW","MKT_PHASE_CD"=EXCLUDED."MKT_PHASE_CD",
             "REG_BUY_SIG_RAW"=EXCLUDED."REG_BUY_SIG_RAW","REG_BUY_SIG_CD"=EXCLUDED."REG_BUY_SIG_CD",
             "ADD_BUY_SIG_RAW"=EXCLUDED."ADD_BUY_SIG_RAW","ADD_BUY_SIG_CD"=EXCLUDED."ADD_BUY_SIG_CD",
             "CASH_INPUT_RT"=EXCLUDED."CASH_INPUT_RT","EXTRACTED_JSON"=EXCLUDED."EXTRACTED_JSON",
             "EXTRACT_CONF_RT"=EXCLUDED."EXTRACT_CONF_RT","REVIEW_STS"=EXCLUDED."REVIEW_STS",
             "REVIEW_NOTE"=EXCLUDED."REVIEW_NOTE","UPD_DTTM"=CURRENT_TIMESTAMP
            """)
        .param("id", legacyId)
        .param("risk", risk)
        .param("phaseRaw", phase)
        .param("phase", phase)
        .param("regularRaw", regular.raw())
        .param("regular", regular.code())
        .param("additionalRaw", additional.raw())
        .param("additional", additional.code())
        .param("cash", cash)
        .param("json", json)
        .param("confidence", confidence)
        .param("reviewStatus", review ? "REVIEW_REQUIRED" : "AUTO")
        .param("reviewNote", review ? "서로 다른 위험지수 후보가 원문에 둘 이상 있어 대표값 검토가 필요합니다." : null)
        .update();
    return review;
  }

  private List<Integer> riskCandidates(String text) {
    Set<Integer> values = new LinkedHashSet<>();
    Matcher matcher = RISK_PATTERN.matcher(text);
    while (matcher.find()) {
      int value = Integer.parseInt(matcher.group(1));
      if (value <= 100) values.add(value);
    }
    return new ArrayList<>(values);
  }

  private String phaseCode(String text) {
    if (contains(text, "폭락", "패닉장")) return "CRASH_RISK";
    if (contains(text, "강한 조정", "급락장")) return "STRONG_CORRECTION";
    if (contains(text, "단기 조정", "조정 국면", "조정이 시작")) return "MILD_CORRECTION";
    if (contains(text, "과열", "쏠림 심화")) return "OVERHEATED";
    if (contains(text, "정상", "상승 추세", "상승추세")) return "NORMAL";
    return null;
  }

  private Signal regularBuySignal(String text) {
    if (contains(text, "정기매수 중단", "정기매수 일시정지"))
      return new Signal("정기매수 중단/일시정지", "PAUSE_REGULAR_BUY");
    if (Pattern.compile("정기매수[^\\n]{0,40}(50|절반|축소|낮추)").matcher(text).find())
      return new Signal("정기매수 감액", "REDUCE_REGULAR_BUY");
    if (contains(text, "정기매수는 유지", "정기매수 유지", "정기 매수 유지"))
      return new Signal("정기매수 유지", "KEEP_REGULAR_BUY");
    return new Signal(null, null);
  }

  private Signal additionalBuySignal(String text) {
    if (contains(text, "추가매수는 아직", "추가 매수는 아직", "추가매수보다", "공격 매수를 피"))
      return new Signal("추가매수 대기", "WAIT_ADDITIONAL_BUY");
    if (contains(text, "분할매수", "분할 매수")) return new Signal("분할 추가매수", "SCALE_IN_ADDITIONAL_BUY");
    return new Signal(null, null);
  }

  private BigDecimal cashRate(String text) {
    Matcher matcher = CASH_PATTERN.matcher(text);
    if (!matcher.find()) return null;
    BigDecimal first = new BigDecimal(matcher.group(1));
    return matcher.group(2) == null
        ? first
        : first.add(new BigDecimal(matcher.group(2))).divide(BigDecimal.valueOf(2));
  }

  private boolean contains(String text, String... values) {
    for (String value : values) if (text.contains(value)) return true;
    return false;
  }

  private String recoveryStatus(String source, String original) {
    if (original != null) return "RECOVERED";
    return source != null && source.contains("불완전") ? "INCOMPLETE" : "MISSING";
  }

  private String briefingType(String value) {
    if (value == null) return null;
    return switch (value) {
      case "일일" -> "DAILY";
      case "주간" -> "WEEKLY";
      case "월간" -> "MONTHLY";
      default -> "AD_HOC";
    };
  }

  private String text(JsonNode node, String field) {
    String value = nullableText(node, field);
    return value == null ? "" : value;
  }

  private String nullableText(JsonNode node, String field) {
    if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) return null;
    String value = node.path(field).asText().trim();
    return value.isEmpty() ? null : value;
  }

  private String sha256(String value) {
    try {
      byte[] bytes =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
    }
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (IOException exception) {
      throw new IllegalStateException("추출 결과 JSON을 생성할 수 없습니다.", exception);
    }
  }

  private record Signal(String raw, String code) {}

  public record ImportResult(
      UUID batchId, int total, int recovered, int incomplete, int missing, int reviewRequired) {}
}
