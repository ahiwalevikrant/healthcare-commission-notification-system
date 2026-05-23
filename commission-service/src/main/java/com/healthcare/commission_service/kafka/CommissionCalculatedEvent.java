package com.healthcare.commission_service.kafka;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionCalculatedEvent {

    private String agentNpn;
    private String agentEmail;
    private String carrierId;
    private String month;
    private BigDecimal totalPayout;
    private String status;
}
