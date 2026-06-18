package com.healthcare.ner_service.groq;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroqAnalysisResult {
    private String validity;
    private String recommendedAction;
    private String reason;
    private String priority;

    public static GroqAnalysisResult defaultResult() {
        return GroqAnalysisResult.builder()
                .validity("NEEDS_REVIEW")
                .recommendedAction("INVESTIGATE")
                .reason("Automated analysis unavailable")
                .priority("MEDIUM")
                .build();
    }
}