package com.nanum.investment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_brf_dtl",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_brf_dtl_01",
                columnNames = {"brf_id", "item_cd"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TbBrfDtl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dtl_id")
    private Long dtlId;

    @Column(name = "brf_id", nullable = false)
    private Long brfId;

    @Column(name = "item_cd", nullable = false, length = 30)
    private String itemCd;

    @Column(name = "item_sum", length = 1000)
    private String itemSum;

    @Column(name = "item_cont", nullable = false, columnDefinition = "TEXT")
    private String itemCont;

    @Column(name = "signal_cd", length = 20)
    private String signalCd;

    @Column(name = "act_yn", nullable = false, length = 1)
    @Builder.Default
    private String actYn = "N";

    @Column(name = "reg_dt", nullable = false, insertable = false, updatable = false)
    private LocalDateTime regDt;

    @Column(name = "mod_dt", nullable = false, insertable = false)
    private LocalDateTime modDt;

    @PreUpdate
    void touchModDt() {
        modDt = LocalDateTime.now();
    }
}
