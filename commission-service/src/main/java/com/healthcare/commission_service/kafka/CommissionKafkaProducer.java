package com.healthcare.commission_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommissionKafkaProducer {

    private final KafkaTemplate<String, CommissionCalculatedEvent> kafkaTemplate;

    @Value("${kafka.topic.commission-calculated}")
    private String topic;

    public void publishCommissionCalculated(CommissionCalculatedEvent event) {
        kafkaTemplate.send(topic, event.getAgentNpn(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish commission event for NPN: {}",
                                event.getAgentNpn(), ex);
                    } else {
                        log.info("Commission event published for NPN: {} month: {}",
                                event.getAgentNpn(), event.getMonth());
                    }
                });
    }
}
