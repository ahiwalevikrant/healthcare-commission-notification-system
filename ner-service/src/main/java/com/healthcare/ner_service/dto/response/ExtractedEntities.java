package com.healthcare.ner_service.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedEntities {
    private String agentName;
    private String agentNpn;
    private String carrierName;
    private String policyId;
    private String state;
    private String month;
}
