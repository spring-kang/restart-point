package com.restartpoint.domain.admin.dto;

import com.restartpoint.domain.payment.entity.Order;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminOrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String userName;
    private String userEmail;
    private String planName;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private LocalDateTime createdAt;

    public static AdminOrderResponse from(Order order) {
        return AdminOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName())
                .userEmail(order.getUser().getEmail())
                .planName(order.getPricingPlan().getName())
                .amount(order.getAmount())
                .status(order.getStatus().name())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
