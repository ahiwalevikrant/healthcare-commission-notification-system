package com.healthcare.ner_service.repository;

import com.healthcare.ner_service.entity.DisputeRecord;
import com.healthcare.ner_service.entity.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeRepository extends JpaRepository<DisputeRecord, Long> {
    Page<DisputeRecord> findByAgentNpn(String agentNpn, Pageable pageable);
    Page<DisputeRecord> findByStatus(DisputeStatus status, Pageable pageable);
    Page<DisputeRecord> findByCarrierName(String carrierName, Pageable pageable);
}