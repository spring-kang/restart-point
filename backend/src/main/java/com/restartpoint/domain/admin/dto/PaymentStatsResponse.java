package com.restartpoint.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PaymentStatsResponse {
    private BigDecimal totalRevenue;
    private int activeSubscriptions;
    private int pendingOrders;
    private int completedOrders;
    private int cancelledOrders;
    private int refundedOrders;
}
