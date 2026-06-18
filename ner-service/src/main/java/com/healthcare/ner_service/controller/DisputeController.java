package com.healthcare.ner_service.controller;

import com.healthcare.ner_service.dto.request.DisputeRequest;
import com.healthcare.ner_service.dto.response.ApiResponse;
import com.healthcare.ner_service.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping("/analyse")
    public ResponseEntity<ApiResponse<?>> analyse(
            @Valid @RequestBody DisputeRequest request) {
        return ResponseEntity.ok(disputeService.analyse(request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(
            @RequestParam(required = false) String agentNpn,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(disputeService.getAll(
                agentNpn, status,
                PageRequest.of(page, size,
                        Sort.by("extractedAt").descending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(disputeService.getById(id));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<?>> resolve(@PathVariable Long id) {
        return ResponseEntity.ok(disputeService.resolve(id));
    }
}
