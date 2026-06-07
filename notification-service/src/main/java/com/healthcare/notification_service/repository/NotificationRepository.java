package com.healthcare.notification_service.repository;


import com.healthcare.notification_service.entity.NotificationLog;
import com.healthcare.notification_service.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationLog, Long>{

    Page<NotificationLog> findByAgentNpn(String agentNpn, Pageable pageable);

    Page<NotificationLog> findByStatus(NotificationStatus status, Pageable pageable);

    Page<NotificationLog> findByAgentNpnAndStatus(
            String agentNpn, NotificationStatus status, Pageable pageable);
}
