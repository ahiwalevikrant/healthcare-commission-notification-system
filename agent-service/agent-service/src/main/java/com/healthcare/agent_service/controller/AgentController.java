package com.healthcare.agent_service.controller;

import com.healthcare.agent_service.dto.request.LoginRequest;
import com.healthcare.agent_service.dto.request.RegisterRequest;
import com.healthcare.agent_service.dto.response.ApiResponse;
import com.healthcare.agent_service.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(agentService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(agentService.login(request));
    }

    @GetMapping("/{npn}")
    public ResponseEntity<ApiResponse<?>> getByNpn(@PathVariable String npn) {
        return ResponseEntity.ok(agentService.getByNpn(npn));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(agentService.getAll(PageRequest.of(page, size)));
    }

}
