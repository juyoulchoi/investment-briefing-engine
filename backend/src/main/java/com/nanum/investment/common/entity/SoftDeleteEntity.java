package com.nanum.investment.common.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class SoftDeleteEntity extends BaseEntity {
    @Column(name = "USE_YN", nullable = false, length = 1)
    private String useYn = "Y";
    @Column(name = "DEL_YN", nullable = false, length = 1)
    private String deleteYn = "N";

    public void activate() {
        useYn = "Y";
    }

    public void deactivate() {
        useYn = "N";
    }

    public void softDelete() {
        deleteYn = "Y";
        useYn = "N";
    }

    public boolean isDeleted() {
        return "Y".equals(deleteYn);
    }
}
