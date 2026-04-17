package com.restartpoint.domain.payment.repository;

import com.restartpoint.domain.payment.entity.PricingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PricingPlanRepository extends JpaRepository<PricingPlan, Long> {

    List<PricingPlan> findBySeasonIdAndActiveTrueOrderByDisplayOrderAsc(Long seasonId);

    List<PricingPlan> findBySeasonIdOrderByDisplayOrderAsc(Long seasonId);

    Optional<PricingPlan> findBySeasonIdAndName(Long seasonId, String name);

    Optional<PricingPlan> findBySeasonIdAndRecommendedTrue(Long seasonId);

    boolean existsBySeasonIdAndName(Long seasonId, String name);
}
