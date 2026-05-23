package com.healthcare.agent_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "NPN is required")
    private String npn;

    @NotBlank(message = "Password is required")
    private String password;

}
