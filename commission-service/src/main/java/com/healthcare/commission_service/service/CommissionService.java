package com.healthcare.commission_service.service;

import com.healthcare.commission_service.dto.request.CommissionRequest;
import com.healthcare.commission_service.dto.response.ApiResponse;
import org.springframework.data.domain.Pageable;


public interface CommissionService {

    ApiResponse<?> submit(CommissionRequest request);
    ApiResponse<?> calculate(Long id);
    ApiResponse<?> flagDispute(Long id);
    ApiResponse<?> getAll(String agentNpn, String status, String month, Pageable pageable);
    ApiResponse<?> getSummary(String agentNpn, String month);

}
