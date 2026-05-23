package com.healthcare.commission_service.repository;

import com.healthcare.commission_service.entity.CommissionRecord;
import com.healthcare.commission_service.entity.CommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface CommissionRepository extends JpaRepository<CommissionRecord, Long> {


    Page<CommissionRecord> findByAgentNpn(String agentNpn, Pageable pageable);

    Page<CommissionRecord> findByStatus(CommissionStatus status, Pageable pageable);

    Page<CommissionRecord> findByAgentNpnAndStatus(
            String agentNpn, CommissionStatus status, Pageable pageable);

    Page<CommissionRecord> findByAgentNpnAndMonth(
            String agentNpn, String month, Pageable pageable);

    Page<CommissionRecord> findByAgentNpnAndStatusAndMonth(
            String agentNpn, CommissionStatus status, String month, Pageable pageable);


    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CommissionRecord c " +
            "WHERE c.agentNpn = :npn AND c.month = :month AND c.status = :status")
    BigDecimal sumAmountByAgentNpnAndMonthAndStatus(
            @Param("npn") String npn,
            @Param("month") String month,
            @Param("status") CommissionStatus status);

    Optional<CommissionRecord> findByPolicyIdAndMonth(String policyId, String month);

}
