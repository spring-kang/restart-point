package com.restartpoint.domain.payment.repository;

import com.restartpoint.domain.payment.entity.Order;
import com.restartpoint.domain.payment.entity.Order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    @Query("SELECT o FROM Order o " +
           "JOIN o.pricingPlan pp " +
           "WHERE pp.season.id = :seasonId " +
           "ORDER BY o.createdAt DESC")
    List<Order> findBySeasonId(@Param("seasonId") Long seasonId);

    @Query("SELECT o FROM Order o " +
           "JOIN o.pricingPlan pp " +
           "WHERE pp.season.id = :seasonId AND o.status = :status")
    List<Order> findBySeasonIdAndStatus(
            @Param("seasonId") Long seasonId,
            @Param("status") OrderStatus status);

    Optional<Order> findByPaymentKey(String paymentKey);

    boolean existsByOrderNumber(String orderNumber);
}
