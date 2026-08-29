package com.nanum.investment.marketdata.application;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataCorrectionManagementService {
  private final JdbcClient jdbc;
  public DataCorrectionManagementService(JdbcClient jdbc) { this.jdbc = jdbc; }

  public List<Map<String, Object>> corrections(int limit) {
    return jdbc.sql("SELECT \"CORR_ID\" correction_id,\"PROVIDER_CD\" provider_code,\"DATASET_CD\" dataset_code,\"SOURCE_KEY\" source_key,\"BASE_DT\" base_date,\"CHANGED_COLS\" changed_columns,\"PAYLOAD_VER\" payload_version,\"RECOLLECT_JOB_ID\" recollect_job_id,\"CORR_REASON\" correction_reason,\"DETECTED_DTTM\" detected_at FROM \"TB_DATA_CORR_HIS\" ORDER BY \"CORR_ID\" DESC LIMIT :limit")
        .param("limit", Math.min(Math.max(limit, 1), 1000)).query().listOfRows();
  }

  public List<Map<String, Object>> recalculations(String requestedStatus, int limit) {
    String status = requestedStatus == null ? "PENDING" : requestedStatus.toUpperCase();
    return jdbc.sql("SELECT \"RECALC_ID\" recalculation_id,\"CORR_ID\" correction_id,\"TARGET_TP\" target_type,\"BASE_DT\" base_date,\"STATUS\" status,\"ATTEMPT_CNT\" attempt_count,\"ERROR_MSG\" error_message,\"CRT_DTTM\" created_at,\"START_DTTM\" started_at,\"COMPLETE_DTTM\" completed_at FROM \"TB_RECALC_QUEUE\" WHERE \"STATUS\"=:status ORDER BY \"RECALC_ID\" LIMIT :limit")
        .param("status", status).param("limit", Math.min(Math.max(limit, 1), 1000)).query().listOfRows();
  }

  @Transactional
  public void transition(long id, String requestedStatus, String error) {
    String status = requestedStatus.toUpperCase();
    if (!List.of("RUNNING","COMPLETED","FAILED","IGNORED","PENDING").contains(status))
      throw new IllegalArgumentException("지원하지 않는 재계산 상태입니다: " + status);
    int updated = jdbc.sql("UPDATE \"TB_RECALC_QUEUE\" SET \"STATUS\"=:status,\"ATTEMPT_CNT\"=CASE WHEN :status='RUNNING' THEN \"ATTEMPT_CNT\"+1 ELSE \"ATTEMPT_CNT\" END,\"START_DTTM\"=CASE WHEN :status='RUNNING' THEN CURRENT_TIMESTAMP ELSE \"START_DTTM\" END,\"COMPLETE_DTTM\"=CASE WHEN :status IN ('COMPLETED','FAILED','IGNORED') THEN CURRENT_TIMESTAMP ELSE NULL END,\"ERROR_MSG\"=:error WHERE \"RECALC_ID\"=:id")
        .param("status", status).param("error", error).param("id", id).update();
    if (updated == 0) throw new IllegalArgumentException("재계산 대상을 찾을 수 없습니다: " + id);
  }
}
