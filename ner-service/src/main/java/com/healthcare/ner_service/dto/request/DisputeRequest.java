package com.healthcare.ner_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DisputeRequest {

    @NotBlank(message = "Dispute text is required")
    private String text;
}
