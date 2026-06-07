package com.healthcare.notification_service.controller;

import com.healthcare.notification_service.dto.ApiResponse;
import com.healthcare.notification_service.entity.NotificationStatus;
import com.healthcare.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(
            @RequestParam(required = false) String agentNpn,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        var pageable = PageRequest.of(page, size,
                Sort.by("sentAt").descending());

        var records = agentNpn != null && status != null
                ? notificationRepository.findByAgentNpnAndStatus(
                agentNpn, NotificationStatus.valueOf(status), pageable)
                : agentNpn != null
                ? notificationRepository.findByAgentNpn(agentNpn, pageable)
                : status != null
                ? notificationRepository.findByStatus(
                NotificationStatus.valueOf(status), pageable)
                : notificationRepository.findAll(pageable);

        return ResponseEntity.ok(ApiResponse.success(records, "Notifications fetched"));
    }

}
