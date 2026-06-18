package com.healthcare.ner_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispute_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Raw input
    @Column(nullable = false, length = 2000)
    private String rawText;

    // Extracted by NLP + Regex
    private String agentNpn;
    private String agentName;
    private String carrierName;
    private String policyId;
    private String state;
    private String month;

    // Groq AI analysis
    @Enumerated(EnumType.STRING)
    private DisputeValidity validity;

    private String recommendedAction;
    private String aiReason;

    @Enumerated(EnumType.STRING)
    private DisputePriority priority;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status;

    @Column(updatable = false)
    private LocalDateTime extractedAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        this.extractedAt = LocalDateTime.now();
        if (this.status == null) this.status = DisputeStatus.OPEN;
    }
}
