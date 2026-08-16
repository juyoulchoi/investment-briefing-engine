package com.nanum.investment.domain;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "\"TB_ERR_LOG\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbErrLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ERR_LOG_ID")
  private Long errorLogId;

  @Column(name = "TRACE_ID", length = 100)
  private String traceId;

  @Column(name = "ERR_DTTM", nullable = false)
  private OffsetDateTime errorDateTime;

  @Enumerated(EnumType.STRING)
  @Column(name = "ERR_LVL", nullable = false, length = 20)
  private ErrorLevel errorLevel;

  @Column(name = "ERR_GRP_CD", nullable = false, length = 50)
  private String errorGroupCode;

  @Column(name = "ERR_CD", length = 100)
  private String errorCode;

  @Column(name = "ERR_MSG", nullable = false, length = 2000)
  private String errorMessage;

  @Column(name = "EXC_CLASS_NM", length = 300)
  private String exceptionClassName;

  @Column(name = "STACK_TRACE", columnDefinition = "TEXT")
  private String stackTrace;

  @Builder.Default
  @Column(name = "RESOLVED_YN", nullable = false, length = 1)
  private String resolvedYn = "N";

  @Column(name = "RESOLVED_DTTM")
  private OffsetDateTime resolvedDateTime;

  @Column(name = "RESOLVED_USR_ID", length = 100)
  private String resolvedUserId;

  @Column(name = "RESOLUTION_MEMO", length = 2000)
  private String resolutionMemo;
}
