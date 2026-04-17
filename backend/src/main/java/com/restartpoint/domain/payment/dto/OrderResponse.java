package com.restartpoint.domain.payment.dto;

import com.restartpoint.domain.payment.entity.Order;
import com.restartpoint.domain.payment.entity.Order.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private Long userId;
    private String userName;
    private Long pricingPlanId;
    private String pricingPlanName;
    private Long seasonId;
    private String seasonTitle;
    private BigDecimal amount;
    private OrderStatus status;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private String refundReason;
    private LocalDateTime createdAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName())
                .pricingPlanId(order.getPricingPlan().getId())
                .pricingPlanName(order.getPricingPlan().getName())
                .seasonId(order.getPricingPlan().getSeason().getId())
                .seasonTitle(order.getPricingPlan().getSeason().getTitle())
                .amount(order.getAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paidAt(order.getPaidAt())
                .refundedAt(order.getRefundedAt())
                .refundReason(order.getRefundReason())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
