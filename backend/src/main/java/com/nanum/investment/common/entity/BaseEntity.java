package com.nanum.investment.common.entity;
import jakarta.persistence.*; import java.time.OffsetDateTime; import lombok.Getter; import org.springframework.data.annotation.*; import org.springframework.data.jpa.domain.support.AuditingEntityListener;
@Getter @MappedSuperclass @EntityListeners(AuditingEntityListener.class) public abstract class BaseEntity {
 @CreatedDate @Column(name="CRT_DTTM",nullable=false,updatable=false) private OffsetDateTime createdDateTime;
 @CreatedBy @Column(name="CRT_USR_ID",length=50,updatable=false) private String createdUserId;
 @LastModifiedDate @Column(name="UPD_DTTM",nullable=false) private OffsetDateTime updatedDateTime;
 @LastModifiedBy @Column(name="UPD_USR_ID",length=50) private String updatedUserId;
}
