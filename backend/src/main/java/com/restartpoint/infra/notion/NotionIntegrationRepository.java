package com.restartpoint.infra.notion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotionIntegrationRepository extends JpaRepository<NotionIntegration, Long> {

    Optional<NotionIntegration> findByUserId(Long userId);

    Optional<NotionIntegration> findByUserIdAndActiveTrue(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByUserIdAndActiveTrue(Long userId);

    void deleteByUserId(Long userId);
}
