package com.healthcare.commission_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CommissionRequest {

    @NotBlank(message = "Agent NPN is required")
    private String agentNpn;

    @NotBlank(message = "Carrier ID is required")
    private String carrierId;

    @NotBlank(message = "Policy ID is required")
    private String policyId;

    @NotBlank(message = "Month is required — format: yyyy-MM")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
            message = "Month must be in format yyyy-MM")
    private String month;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private java.math.BigDecimal amount;

    @NotNull(message = "Commission type is required")
    private com.healthcare.commission_service.entity.CommissionType commissionType;

}
