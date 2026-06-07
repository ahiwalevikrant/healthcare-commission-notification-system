package com.healthcare.notification_service.kafka;

import com.healthcare.notification_service.entity.NotificationLog;
import com.healthcare.notification_service.entity.NotificationStatus;
import com.healthcare.notification_service.repository.NotificationRepository;
import com.healthcare.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommissionEventConsumer {
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @KafkaListener(
            topics = "${kafka.topic.commission-calculated}",
            groupId = "notification-group"
    )
    public void consume(CommissionCalculatedEvent event) {
        log.info("Received commission event for NPN: {} month: {}",
                event.getAgentNpn(), event.getMonth());

        NotificationLog log = NotificationLog.builder()
                .agentNpn(event.getAgentNpn())
                .carrierId(event.getCarrierId())
                .month(event.getMonth())
                .amount(event.getTotalPayout())
                .agentEmail(event.getAgentEmail())
                .build();

        try {
            emailService.sendPayoutNotification(
                    event.getAgentEmail(),
                    event.getAgentNpn(),
                    event.getCarrierId(),
                    event.getMonth(),
                    event.getTotalPayout()
            );

            log.setStatus(NotificationStatus.SENT);
            notificationRepository.save(log);
            this.log.info("Notification saved with status SENT for NPN: {}",
                    event.getAgentNpn());

        } catch (Exception e) {
            log.setStatus(NotificationStatus.FAILED);
            log.setErrorMessage(e.getMessage());
            notificationRepository.save(log);
            this.log.error("Notification failed for NPN: {}", event.getAgentNpn());
        }
    }

}
