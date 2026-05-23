package com.healthcare.agent_service.service.impl;

import com.healthcare.agent_service.dto.request.LoginRequest;
import com.healthcare.agent_service.dto.request.RegisterRequest;
import com.healthcare.agent_service.dto.response.ApiResponse;
import com.healthcare.agent_service.entity.Agent;
import com.healthcare.agent_service.repository.AgentRepository;
import com.healthcare.agent_service.security.JwtUtil;
import com.healthcare.agent_service.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public ApiResponse<?> register(RegisterRequest request) {
        if (agentRepository.existsByNpn(request.getNpn())) {
            return ApiResponse.error("NPN already registered");
        }
        if (agentRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.error("Email already registered");
        }

        Agent agent = Agent.builder()
                .name(request.getName())
                .npn(request.getNpn())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .state(request.getState())
                .licenseNumber(request.getLicenseNumber())
                .build();

        agentRepository.save(agent);

        return ApiResponse.success(
                Map.of("npn", agent.getNpn(), "name", agent.getName()),
                "Agent registered successfully"
        );
    }

    @Override
    public ApiResponse<?> login(LoginRequest request) {
        Agent agent = agentRepository.findByNpn(request.getNpn())
                .orElse(null);

        if (agent == null || !passwordEncoder.matches(request.getPassword(), agent.getPassword())) {
            return ApiResponse.error("Invalid NPN or password");
        }

        String token = jwtUtil.generateToken(agent.getNpn());

        return ApiResponse.success(
                Map.of(
                        "token", token,
                        "npn", agent.getNpn(),
                        "name", agent.getName()
                ),
                "Login successful"
        );
    }

    @Override
    public ApiResponse<?> getByNpn(String npn) {
        return agentRepository.findByNpn(npn)
                .map(agent -> ApiResponse.success(agent, "Agent found"))
                .orElse(ApiResponse.error("Agent not found"));
    }

    @Override
    public ApiResponse<?> getAll(Pageable pageable) {
        return ApiResponse.success(agentRepository.findAll(pageable), "Agents fetched");
    }


}
