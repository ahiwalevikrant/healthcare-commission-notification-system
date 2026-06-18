package com.healthcare.ner_service.service;

import com.healthcare.ner_service.dto.request.DisputeRequest;
import com.healthcare.ner_service.dto.response.ApiResponse;
import org.springframework.data.domain.Pageable;

public interface DisputeService {
    ApiResponse<?> analyse(DisputeRequest request);
    ApiResponse<?> getAll(String agentNpn, String status, Pageable pageable);
    ApiResponse<?> getById(Long id);
    ApiResponse<?> resolve(Long id);
}
