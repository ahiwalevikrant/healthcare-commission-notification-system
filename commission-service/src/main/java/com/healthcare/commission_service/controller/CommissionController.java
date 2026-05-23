package com.healthcare.commission_service.controller;

import com.healthcare.commission_service.dto.request.CommissionRequest;
import com.healthcare.commission_service.dto.response.ApiResponse;
import com.healthcare.commission_service.service.CommissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> submit(
            @Valid @RequestBody CommissionRequest request) {
        return ResponseEntity.ok(commissionService.submit(request));
    }

    @PutMapping("/{id}/calculate")
    public ResponseEntity<ApiResponse<?>> calculate(@PathVariable Long id) {
        return ResponseEntity.ok(commissionService.calculate(id));
    }

    @PutMapping("/{id}/flag-dispute")
    public ResponseEntity<ApiResponse<?>> flagDispute(@PathVariable Long id) {
        return ResponseEntity.ok(commissionService.flagDispute(id));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(
            @RequestParam(required = false) String agentNpn,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(commissionService.getAll(
                agentNpn, status, month,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<?>> getSummary(
            @RequestParam String agentNpn,
            @RequestParam String month) {
        return ResponseEntity.ok(commissionService.getSummary(agentNpn, month));
    }

}
