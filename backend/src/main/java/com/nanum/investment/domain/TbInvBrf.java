package com.nanum.investment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"TB_BRF\"")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TbInvBrf {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BRF_ID")
    private Long brfId;

    @Column(name = "BASE_DT", nullable = false)
    private LocalDate baseDate;

    @Column(name = "TITLE", nullable = false, length = 300)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "BRF_STS", nullable = false, length = 20)
    @Builder.Default
    private BriefingStatus briefingStatus = BriefingStatus.READY;

    @Enumerated(EnumType.STRING) @Builder.Default @Column(name="BRF_TP",nullable=false) private BriefingType briefingType=BriefingType.DAILY;
    @Enumerated(EnumType.STRING) @Builder.Default @Column(name="SCOPE_TP",nullable=false) private BriefingScopeType scopeType=BriefingScopeType.GLOBAL;
    @Enumerated(EnumType.STRING) @Builder.Default @Column(name="DATA_STS",nullable=false) private DataStatus dataStatus=DataStatus.FRESH;
    @Builder.Default @Column(name="CONF_RT",nullable=false) private java.math.BigDecimal confidenceRate=new java.math.BigDecimal("100");
    @Builder.Default @Column(name="CALC_SEQ",nullable=false) private Integer calculationSequence=1;
    @Builder.Default @Column(name="LATEST_YN",nullable=false) private String latestYn="Y";
    @Builder.Default @Column(name="PUBL_YN",nullable=false) private String publishedYn="N";

    @Column(name = "CRT_DTTM", nullable = false, insertable = false, updatable = false)
    private LocalDateTime regDt;

    @Column(name = "UPD_DTTM", nullable = false, insertable = false)
    private LocalDateTime modDt;

    @PreUpdate
    void touchModDt() {
        modDt = LocalDateTime.now();
    }
}



