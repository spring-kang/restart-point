package com.restartpoint.domain.review.repository;

import com.restartpoint.domain.review.entity.ReviewGuideCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewGuideCompletionRepository extends JpaRepository<ReviewGuideCompletion, Long> {

    Optional<ReviewGuideCompletion> findByUserId(Long userId);

    boolean existsByUserIdAndFullyCompletedTrue(Long userId);
}
