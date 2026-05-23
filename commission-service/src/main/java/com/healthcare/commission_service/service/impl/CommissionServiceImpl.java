package com.healthcare.commission_service.service.impl;

import com.healthcare.commission_service.dto.request.CommissionRequest;
import com.healthcare.commission_service.dto.response.ApiResponse;
import com.healthcare.commission_service.entity.CommissionRecord;
import com.healthcare.commission_service.entity.CommissionStatus;
import com.healthcare.commission_service.kafka.CommissionCalculatedEvent;
import com.healthcare.commission_service.kafka.CommissionKafkaProducer;
import com.healthcare.commission_service.repository.CommissionRepository;
import com.healthcare.commission_service.service.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionServiceImpl implements CommissionService {

    private final CommissionRepository commissionRepository;
    private final CommissionKafkaProducer kafkaProducer;

    @Override
    public ApiResponse<?> submit(CommissionRequest request) {

        // Business rule 1: amount must be greater than 0
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error("Commission amount must be greater than 0");
        }

        // Business rule 2: no duplicate policy + month
        boolean exists = commissionRepository
                .findByPolicyIdAndMonth(request.getPolicyId(), request.getMonth())
                .isPresent();
        if (exists) {
            return ApiResponse.error("Commission already submitted for this policy and month");
        }

        CommissionRecord record = CommissionRecord.builder()
                .agentNpn(request.getAgentNpn())
                .carrierId(request.getCarrierId())
                .policyId(request.getPolicyId())
                .month(request.getMonth())
                .amount(request.getAmount())
                .commissionType(request.getCommissionType())
                .status(CommissionStatus.PENDING)
                .build();

        commissionRepository.save(record);

        return ApiResponse.success(record, "Commission submitted successfully");
    }

    @Override
    public ApiResponse<?> calculate(Long id) {

        CommissionRecord record = commissionRepository.findById(id)
                .orElse(null);

        if (record == null) {
            return ApiResponse.error("Commission record not found");
        }

        // Business rule 3: only PENDING records can be calculated
        if (!record.getStatus().equals(CommissionStatus.PENDING)) {
            return ApiResponse.error("Only PENDING commissions can be calculated. " +
                    "Current status: " + record.getStatus());
        }

        // Business rule 4: amount must still be valid
        if (record.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error("Cannot calculate commission with zero or negative amount");
        }

        // Calculate based on commission type
        BigDecimal calculatedAmount = applyCommissionType(record);
        record.setAmount(calculatedAmount);
        record.setStatus(CommissionStatus.CALCULATED);
        commissionRepository.save(record);

        // Publish Kafka event
        CommissionCalculatedEvent event = CommissionCalculatedEvent.builder()
                .agentNpn(record.getAgentNpn())
                .carrierId(record.getCarrierId())
                .month(record.getMonth())
                .totalPayout(record.getAmount())
                .status(record.getStatus().name())
                .build();

        kafkaProducer.publishCommissionCalculated(event);

        log.info("Commission calculated for policy: {} agent: {}",
                record.getPolicyId(), record.getAgentNpn());

        return ApiResponse.success(record, "Commission calculated successfully");
    }

    @Override
    public ApiResponse<?> flagDispute(Long id) {

        CommissionRecord record = commissionRepository.findById(id)
                .orElse(null);

        if (record == null) {
            return ApiResponse.error("Commission record not found");
        }

        // Business rule 5: cannot flag PAID records
        if (record.getStatus().equals(CommissionStatus.PAID)) {
            return ApiResponse.error("Cannot flag a PAID commission for dispute");
        }

        record.setFlagReason("Flagged for dispute review");
        record.setStatus(CommissionStatus.PENDING);
        commissionRepository.save(record);

        return ApiResponse.success(record, "Commission flagged for dispute");
    }

    @Override
    public ApiResponse<?> getAll(String agentNpn, String status,
                                 String month, Pageable pageable) {

        Page<CommissionRecord> records;

        // Filter combinations based on what params are provided
        if (agentNpn != null && status != null && month != null) {
            records = commissionRepository.findByAgentNpnAndStatusAndMonth(
                    agentNpn, CommissionStatus.valueOf(status), month, pageable);

        } else if (agentNpn != null && status != null) {
            records = commissionRepository.findByAgentNpnAndStatus(
                    agentNpn, CommissionStatus.valueOf(status), pageable);

        } else if (agentNpn != null && month != null) {
            records = commissionRepository.findByAgentNpnAndMonth(
                    agentNpn, month, pageable);

        } else if (agentNpn != null) {
            records = commissionRepository.findByAgentNpn(agentNpn, pageable);

        } else if (status != null) {
            records = commissionRepository.findByStatus(
                    CommissionStatus.valueOf(status), pageable);

        } else {
            records = commissionRepository.findAll(pageable);
        }

        return ApiResponse.success(records, "Commission records fetched");
    }

    @Override
    public ApiResponse<?> getSummary(String agentNpn, String month) {

        BigDecimal totalPending = commissionRepository.sumAmountByAgentNpnAndMonthAndStatus(
                agentNpn, month, CommissionStatus.PENDING);

        BigDecimal totalCalculated = commissionRepository.sumAmountByAgentNpnAndMonthAndStatus(
                agentNpn, month, CommissionStatus.CALCULATED);

        BigDecimal totalPaid = commissionRepository.sumAmountByAgentNpnAndMonthAndStatus(
                agentNpn, month, CommissionStatus.PAID);

        Map<String, Object> summary = Map.of(
                "agentNpn", agentNpn,
                "month", month,
                "totalPending", totalPending,
                "totalCalculated", totalCalculated,
                "totalPaid", totalPaid,
                "grandTotal", totalPending.add(totalCalculated).add(totalPaid)
        );

        return ApiResponse.success(summary, "Summary fetched");
    }

    // Apply commission type logic from domain rules
    private BigDecimal applyCommissionType(CommissionRecord record) {
        return switch (record.getCommissionType()) {
            case PERCENT -> record.getAmount(); // amount already is the % value
            case PMPM -> record.getAmount();    // fixed per member per month
            case PMPY -> record.getAmount().divide(BigDecimal.valueOf(12), 2,
                    java.math.RoundingMode.HALF_UP); // annual → monthly
            case PSPY -> record.getAmount().divide(BigDecimal.valueOf(12), 2,
                    java.math.RoundingMode.HALF_UP);
            default -> record.getAmount();
        };
    }

}
