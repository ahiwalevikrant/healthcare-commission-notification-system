package com.healthcare.agent_service.repository;

import com.healthcare.agent_service.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByNpn(String npn);

    Optional<Agent> findByEmail(String email);

    boolean existsByNpn(String npn);

    boolean existsByEmail(String email);


}

