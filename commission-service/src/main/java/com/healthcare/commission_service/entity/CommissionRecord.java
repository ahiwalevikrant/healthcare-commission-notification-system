package com.healthcare.commission_service.entity;


import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String agentNpn;

    @Column(nullable = false)
    private String carrierId;

    @Column(nullable = false)
    private String policyId;

    @Column(nullable = false)
    private String month; // format: 2024-01

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommissionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommissionType commissionType;

    private String flagReason;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = CommissionStatus.PENDING;
        if (this.commissionType == null) this.commissionType = CommissionType.NA;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
