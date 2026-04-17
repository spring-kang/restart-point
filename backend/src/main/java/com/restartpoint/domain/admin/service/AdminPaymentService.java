package com.restartpoint.domain.admin.service;

import com.restartpoint.domain.admin.dto.AdminOrderResponse;
import com.restartpoint.domain.admin.dto.AdminSubscriptionResponse;
import com.restartpoint.domain.admin.dto.PaymentStatsResponse;
import com.restartpoint.domain.payment.dto.OrderResponse;
import com.restartpoint.domain.payment.entity.Order;
import com.restartpoint.domain.payment.entity.Order.OrderStatus;
import com.restartpoint.domain.payment.entity.Subscription;
import com.restartpoint.domain.payment.entity.Subscription.SubscriptionStatus;
import com.restartpoint.domain.payment.repository.OrderRepository;
import com.restartpoint.domain.payment.repository.SubscriptionRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentService {

    private final OrderRepository orderRepository;
    private final SubscriptionRepository subscriptionRepository;

    public List<AdminOrderResponse> getOrdersBySeasonId(Long seasonId) {
        return orderRepository.findBySeasonId(seasonId).stream()
                .map(AdminOrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse refundOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // TODO: 실제 환불 처리 (결제 게이트웨이 API 호출)
        order.markRefunded(reason);

        // 구독 취소
        subscriptionRepository.findByUserIdAndSeasonId(
                order.getUser().getId(),
                order.getPricingPlan().getSeason().getId()
        ).ifPresent(subscription -> subscription.cancel(reason));

        return OrderResponse.from(order);
    }

    public List<AdminSubscriptionResponse> getSubscriptionsBySeasonId(Long seasonId) {
        return subscriptionRepository.findBySeasonIdAndStatus(seasonId, SubscriptionStatus.ACTIVE).stream()
                .map(AdminSubscriptionResponse::from)
                .collect(Collectors.toList());
    }

    public PaymentStatsResponse getPaymentStats(Long seasonId) {
        List<Order> orders = orderRepository.findBySeasonId(seasonId);
        List<Subscription> subscriptions = subscriptionRepository.findBySeasonIdAndStatus(seasonId, SubscriptionStatus.ACTIVE);

        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int pendingOrders = (int) orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .count();

        int completedOrders = (int) orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .count();

        int cancelledOrders = (int) orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
                .count();

        int refundedOrders = (int) orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.REFUNDED)
                .count();

        return PaymentStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .activeSubscriptions(subscriptions.size())
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .refundedOrders(refundedOrders)
                .build();
    }
}
