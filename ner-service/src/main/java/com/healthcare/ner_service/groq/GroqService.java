package com.healthcare.ner_service.groq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.ner_service.dto.response.ExtractedEntities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroqAnalysisResult analyse(String rawText,
                                      ExtractedEntities entities,
                                      String commissionStatus) {
        String prompt = buildPrompt(rawText, entities, commissionStatus);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 300);
            requestBody.put("messages", List.of(
                    Map.of("role", "system",
                            "content", "You are a healthcare insurance dispute analyst. " +
                                    "Always respond with valid JSON only. No extra text."),
                    Map.of("role", "user", "content", prompt)
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl, entity, String.class);

            return parseGroqResponse(response.getBody());

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
            return GroqAnalysisResult.defaultResult();
        }
    }

    private String buildPrompt(String rawText,
                               ExtractedEntities entities,
                               String commissionStatus) {
        return """
                Analyse this insurance commission dispute and return JSON only.

                Dispute Text: "%s"

                Extracted Information:
                - Agent Name: %s
                - Agent NPN: %s
                - Carrier: %s
                - Policy ID: %s
                - State: %s
                - Month: %s
                - Commission Record Status: %s

                Return ONLY this JSON format, no explanation:
                {
                  "validity": "VALID or INVALID or NEEDS_REVIEW",
                  "recommendedAction": "APPROVE or REJECT or INVESTIGATE",
                  "reason": "one sentence explanation",
                  "priority": "HIGH or MEDIUM or LOW"
                }
                """.formatted(
                rawText,
                entities.getAgentName(),
                entities.getAgentNpn(),
                entities.getCarrierName(),
                entities.getPolicyId(),
                entities.getState(),
                entities.getMonth(),
                commissionStatus != null ? commissionStatus : "NOT_FOUND"
        );
    }

    private GroqAnalysisResult parseGroqResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root
                    .path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asText();

            // Clean markdown if present
            content = content.replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode result = objectMapper.readTree(content);

            return GroqAnalysisResult.builder()
                    .validity(result.path("validity").asText("NEEDS_REVIEW"))
                    .recommendedAction(result.path("recommendedAction").asText("INVESTIGATE"))
                    .reason(result.path("reason").asText("Unable to determine"))
                    .priority(result.path("priority").asText("MEDIUM"))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Groq response: {}", e.getMessage());
            return GroqAnalysisResult.defaultResult();
        }
    }
}
