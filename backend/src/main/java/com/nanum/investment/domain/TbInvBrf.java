package com.nanum.investment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_inv_brf")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TbInvBrf {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brf_id")
    private Long brfId;

    @Column(name = "brf_dt", nullable = false, unique = true)
    private LocalDate brfDt;

    @Column(name = "brf_ttl", nullable = false, length = 200)
    private String brfTtl;

    @Column(name = "brf_st", nullable = false, length = 20)
    @Builder.Default
    private String brfSt = BriefingStatus.DRAFT.name();

    @Column(name = "reg_dt", nullable = false, insertable = false, updatable = false)
    private LocalDateTime regDt;

    @Column(name = "mod_dt", nullable = false, insertable = false)
    private LocalDateTime modDt;

    @PreUpdate
    void touchModDt() {
        modDt = LocalDateTime.now();
    }
}
