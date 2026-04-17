package com.restartpoint.domain.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.payment.dto.*;
import com.restartpoint.domain.payment.entity.Order;
import com.restartpoint.domain.payment.entity.Order.OrderStatus;
import com.restartpoint.domain.payment.entity.PricingPlan;
import com.restartpoint.domain.payment.entity.Subscription;
import com.restartpoint.domain.payment.repository.OrderRepository;
import com.restartpoint.domain.payment.repository.PricingPlanRepository;
import com.restartpoint.domain.payment.repository.SubscriptionRepository;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PricingPlanRepository pricingPlanRepository;
    private final OrderRepository orderRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SeasonRepository seasonRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ========== PricingPlan 관련 ==========

    @Transactional
    public PricingPlanResponse createPricingPlan(Long seasonId, PricingPlanRequest request) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEASON_NOT_FOUND));

        if (pricingPlanRepository.existsBySeasonIdAndName(seasonId, request.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PRICING_PLAN);
        }

        PricingPlan plan = PricingPlan.builder()
                .season(season)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .discountPercentage(request.getDiscountPercentage())
                .features(toJson(request.getFeatures()))
                .includeMentoring(request.getIncludeMentoring())
                .includeAiCoaching(request.getIncludeAiCoaching())
                .includeGrowthReport(request.getIncludeGrowthReport())
                .maxTeamParticipation(request.getMaxTeamParticipation())
                .displayOrder(request.getDisplayOrder())
                .recommended(request.getRecommended())
                .build();

        PricingPlan saved = pricingPlanRepository.save(plan);
        return PricingPlanResponse.from(saved);
    }

    public List<PricingPlanResponse> getPricingPlansBySeasonId(Long seasonId) {
        return pricingPlanRepository.findBySeasonIdAndActiveTrueOrderByDisplayOrderAsc(seasonId).stream()
                .map(PricingPlanResponse::from)
                .collect(Collectors.toList());
    }

    public PricingPlanResponse getPricingPlan(Long planId) {
        PricingPlan plan = pricingPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICING_PLAN_NOT_FOUND));
        return PricingPlanResponse.from(plan);
    }

    @Transactional
    public PricingPlanResponse updatePricingPlan(Long planId, PricingPlanRequest request) {
        PricingPlan plan = pricingPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICING_PLAN_NOT_FOUND));

        plan.update(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getOriginalPrice(),
                request.getDiscountPercentage(),
                toJson(request.getFeatures()),
                request.getIncludeMentoring(),
                request.getIncludeAiCoaching(),
                request.getIncludeGrowthReport(),
                request.getMaxTeamParticipation(),
                request.getDisplayOrder(),
                request.getRecommended()
        );

        return PricingPlanResponse.from(plan);
    }

    @Transactional
    public void deletePricingPlan(Long planId) {
        PricingPlan plan = pricingPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICING_PLAN_NOT_FOUND));
        plan.deactivate();
    }

    // ========== Order 관련 ==========

    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        PricingPlan plan = pricingPlanRepository.findById(request.getPricingPlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICING_PLAN_NOT_FOUND));

        // 이미 구독 중인지 확인
        if (subscriptionRepository.hasActiveSubscription(userId, plan.getSeason().getId())) {
            throw new BusinessException(ErrorCode.ALREADY_SUBSCRIBED);
        }

        String orderNumber = generateOrderNumber();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .pricingPlan(plan)
                .amount(plan.getPrice())
                .build();

        Order saved = orderRepository.save(order);

        // 무료 플랜인 경우 즉시 결제 완료 처리
        if (plan.isFree()) {
            saved.markPaid(null, null, "FREE");
            createSubscription(saved);
        }

        return OrderResponse.from(saved);
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return OrderResponse.from(order);
    }

    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return OrderResponse.from(order);
    }

    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse confirmPayment(PaymentConfirmRequest request) {
        Order order = orderRepository.findByOrderNumber(request.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // TODO: 실제 토스페이먼츠 API 호출하여 결제 승인
        // 여기서는 성공했다고 가정
        order.markPaid(request.getPaymentKey(), request.getPaymentKey(), "CARD");

        // 구독 생성
        createSubscription(order);

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        order.markCancelled(reason);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse refundOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // TODO: 실제 환불 처리 (토스페이먼츠 API 호출)
        order.markRefunded(reason);

        // 구독 취소
        subscriptionRepository.findByUserIdAndSeasonId(
                order.getUser().getId(),
                order.getPricingPlan().getSeason().getId()
        ).ifPresent(subscription -> subscription.cancel(reason));

        return OrderResponse.from(order);
    }

    // ========== Subscription 관련 ==========

    public SubscriptionResponse getMySubscription(Long userId, Long seasonId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndSeasonId(userId, seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
        return SubscriptionResponse.from(subscription);
    }

    public List<SubscriptionResponse> getMySubscriptions(Long userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(SubscriptionResponse::from)
                .collect(Collectors.toList());
    }

    public boolean hasActiveSubscription(Long userId, Long seasonId) {
        return subscriptionRepository.hasActiveSubscription(userId, seasonId);
    }

    public boolean hasMentoringAccess(Long userId, Long seasonId) {
        return subscriptionRepository.findActiveByUserIdAndSeasonId(userId, seasonId)
                .map(Subscription::hasMentoring)
                .orElse(false);
    }

    @Transactional
    public SubscriptionResponse cancelSubscription(Long userId, Long seasonId, String reason) {
        Subscription subscription = subscriptionRepository.findActiveByUserIdAndSeasonId(userId, seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        subscription.cancel(reason);
        return SubscriptionResponse.from(subscription);
    }

    // ========== Helper Methods ==========

    private void createSubscription(Order order) {
        PricingPlan plan = order.getPricingPlan();
        Season season = plan.getSeason();

        Subscription subscription = Subscription.builder()
                .user(order.getUser())
                .season(season)
                .pricingPlan(plan)
                .order(order)
                .startDate(LocalDateTime.now())
                .endDate(season.getReviewEndAt())  // 시즌 종료까지 유효
                .build();

        subscriptionRepository.save(subscription);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
