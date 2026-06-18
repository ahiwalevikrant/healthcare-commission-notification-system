package com.healthcare.ner_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DisputeKafkaProducer {

    private final KafkaTemplate<String, DisputeSubmittedEvent> kafkaTemplate;

    @Value("${kafka.topic.dispute-submitted}")
    private String topic;

    public void publishDisputeSubmitted(DisputeSubmittedEvent event) {
        kafkaTemplate.send(topic, event.getAgentNpn(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish dispute event for NPN: {}",
                                event.getAgentNpn(), ex);
                    } else {
                        log.info("Dispute event published for NPN: {}",
                                event.getAgentNpn());
                    }
                });
    }
}
