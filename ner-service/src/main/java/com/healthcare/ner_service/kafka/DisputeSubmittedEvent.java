package com.healthcare.ner_service.kafka;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeSubmittedEvent {
    private String agentNpn;
    private String carrierId;
    private String policyId;
    private String month;
    private String priority;
    private String recommendedAction;
}