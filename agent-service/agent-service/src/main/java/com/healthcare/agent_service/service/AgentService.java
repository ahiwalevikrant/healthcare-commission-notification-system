package com.healthcare.agent_service.service;

import com.healthcare.agent_service.dto.request.LoginRequest;
import com.healthcare.agent_service.dto.request.RegisterRequest;
import com.healthcare.agent_service.dto.response.ApiResponse;
import org.springframework.data.domain.Pageable;

public interface AgentService {
    ApiResponse<?> register(RegisterRequest request);
    ApiResponse<?> login(LoginRequest request);
    ApiResponse<?> getByNpn(String npn);
    ApiResponse<?> getAll(Pageable pageable);
}
