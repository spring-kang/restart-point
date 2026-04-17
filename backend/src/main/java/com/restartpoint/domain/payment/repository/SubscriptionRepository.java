package com.restartpoint.domain.payment.repository;

import com.restartpoint.domain.payment.entity.Subscription;
import com.restartpoint.domain.payment.entity.Subscription.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserIdAndSeasonId(Long userId, Long seasonId);

    Optional<Subscription> findByUserIdAndSeasonIdAndStatus(Long userId, Long seasonId, SubscriptionStatus status);

    List<Subscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<Subscription> findBySeasonIdAndStatus(Long seasonId, SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s " +
           "WHERE s.user.id = :userId AND s.season.id = :seasonId AND s.status = 'ACTIVE'")
    Optional<Subscription> findActiveByUserIdAndSeasonId(
            @Param("userId") Long userId,
            @Param("seasonId") Long seasonId);

    @Query("SELECT COUNT(s) > 0 FROM Subscription s " +
           "WHERE s.user.id = :userId AND s.season.id = :seasonId AND s.status = 'ACTIVE'")
    boolean hasActiveSubscription(@Param("userId") Long userId, @Param("seasonId") Long seasonId);

    @Query("SELECT s FROM Subscription s " +
           "WHERE s.user.id = :userId AND s.status = 'ACTIVE' " +
           "ORDER BY s.createdAt DESC")
    List<Subscription> findActiveByUserId(@Param("userId") Long userId);
}
