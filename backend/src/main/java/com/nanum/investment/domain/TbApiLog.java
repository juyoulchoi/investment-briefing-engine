package com.nanum.investment.domain;
import jakarta.persistence.*; import lombok.*; import java.time.*;
@Entity @Table(name="\"TB_API_LOG\"") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TbApiLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="API_LOG_ID") private Long apiLogId;
 @Column(name="TRACE_ID",nullable=false,length=100) private String traceId;
 @Column(name="API_GRP_CD",nullable=false,length=30) private String apiGroupCode;
 @Column(name="API_NM",nullable=false,length=150) private String apiName;
 @Column(name="HTTP_METHOD",length=10) private String httpMethod;
 @Column(name="REQ_URL",length=2000) private String requestUrl;
 @Column(name="REQ_PARAM",columnDefinition="TEXT") private String requestParameter;
 @Column(name="REQ_BODY",columnDefinition="TEXT") private String requestBody;
 @Column(name="HTTP_STS_CD") private Integer httpStatusCode;
 @Column(name="RSP_BODY",columnDefinition="TEXT") private String responseBody;
 @Column(name="SUCC_YN",nullable=false,length=1) private String successYn;
 @Column(name="ERR_CD",length=100) private String errorCode;
 @Column(name="ERR_MSG",length=2000) private String errorMessage;
 @Column(name="ELAPSED_MS") private Long elapsedMilliseconds;
 @Builder.Default @Column(name="RETRY_CNT",nullable=false) private Integer retryCount=0;
 @Column(name="REQ_DTTM",nullable=false) private OffsetDateTime requestDateTime;
 @Column(name="RSP_DTTM") private OffsetDateTime responseDateTime;
}
