package com.healthcare.commission_service.entity;

public enum CommissionType {

    PERCENT,    // Percentage of policy premium
    PMPM,       // Per Member Per Month
    PSPM,       // Per Selection Per Month
    PMPY,       // Per Member Per Year
    PCPM,       // Per Case Per Month
    PSPY,       // Per Selection Per Year
    PPPM,       // Per Product Per Month
    NA          // Not applicable
}
