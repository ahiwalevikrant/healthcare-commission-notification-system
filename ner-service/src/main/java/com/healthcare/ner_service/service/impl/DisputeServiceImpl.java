package com.healthcare.ner_service.service.impl;

import com.healthcare.ner_service.dto.request.DisputeRequest;
import com.healthcare.ner_service.dto.response.ApiResponse;
import com.healthcare.ner_service.dto.response.ExtractedEntities;
import com.healthcare.ner_service.entity.*;
import com.healthcare.ner_service.groq.*;
import com.healthcare.ner_service.kafka.*;
import com.healthcare.ner_service.nlp.NerExtractionService;
import com.healthcare.ner_service.repository.DisputeRepository;
import com.healthcare.ner_service.service.DisputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final NerExtractionService nerExtractionService;
    private final GroqService groqService;
    private final DisputeKafkaProducer kafkaProducer;

    @Override
    public ApiResponse<?> analyse(DisputeRequest request) {
        log.info("Analysing dispute: {}", request.getText());

        // Step 1: Extract entities using Stanford NLP + Regex
        ExtractedEntities entities = nerExtractionService.extract(request.getText());

        // Step 2: Call Groq for AI analysis
        GroqAnalysisResult groqResult = groqService.analyse(
                request.getText(), entities, null);

        // Step 3: Save dispute record
        DisputeRecord record = DisputeRecord.builder()
                .rawText(request.getText())
                .agentNpn(entities.getAgentNpn())
                .agentName(entities.getAgentName())
                .carrierName(entities.getCarrierName())
                .policyId(entities.getPolicyId())
                .state(entities.getState())
                .month(entities.getMonth())
                .validity(parseValidity(groqResult.getValidity()))
                .recommendedAction(groqResult.getRecommendedAction())
                .aiReason(groqResult.getReason())
                .priority(parsePriority(groqResult.getPriority()))
                .status(DisputeStatus.OPEN)
                .build();

        disputeRepository.save(record);

        // Step 4: Publish Kafka event if HIGH priority
        if (record.getPriority() == DisputePriority.HIGH) {
            DisputeSubmittedEvent event = DisputeSubmittedEvent.builder()
                    .agentNpn(record.getAgentNpn())
                    .carrierId(record.getCarrierName())
                    .policyId(record.getPolicyId())
                    .month(record.getMonth())
                    .priority(record.getPriority().name())
                    .recommendedAction(record.getRecommendedAction())
                    .build();
            kafkaProducer.publishDisputeSubmitted(event);
        }

        log.info("Dispute saved with ID: {} priority: {}",
                record.getId(), record.getPriority());

        return ApiResponse.success(record, "Dispute analysed successfully");
    }

    @Override
    public ApiResponse<?> getAll(String agentNpn, String status, Pageable pageable) {
        var records = agentNpn != null
                ? disputeRepository.findByAgentNpn(agentNpn, pageable)
                : status != null
                ? disputeRepository.findByStatus(
                DisputeStatus.valueOf(status), pageable)
                : disputeRepository.findAll(pageable);

        return ApiResponse.success(records, "Disputes fetched");
    }

    @Override
    public ApiResponse<?> getById(Long id) {
        return disputeRepository.findById(id)
                .map(r -> ApiResponse.success(r, "Dispute found"))
                .orElse(ApiResponse.error("Dispute not found"));
    }

    @Override
    public ApiResponse<?> resolve(Long id) {
        DisputeRecord record = disputeRepository.findById(id)
                .orElse(null);

        if (record == null) return ApiResponse.error("Dispute not found");

        if (record.getStatus() == DisputeStatus.RESOLVED) {
            return ApiResponse.error("Dispute already resolved");
        }

        record.setStatus(DisputeStatus.RESOLVED);
        record.setResolvedAt(LocalDateTime.now());
        disputeRepository.save(record);

        return ApiResponse.success(record, "Dispute resolved successfully");
    }

    private DisputeValidity parseValidity(String value) {
        try { return DisputeValidity.valueOf(value); }
        catch (Exception e) { return DisputeValidity.NEEDS_REVIEW; }
    }

    private DisputePriority parsePriority(String value) {
        try { return DisputePriority.valueOf(value); }
        catch (Exception e) { return DisputePriority.MEDIUM; }
    }
}
