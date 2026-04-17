package com.restartpoint.domain.payment.dto;

import com.restartpoint.domain.payment.entity.Subscription;
import com.restartpoint.domain.payment.entity.Subscription.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SubscriptionResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long seasonId;
    private String seasonTitle;
    private Long pricingPlanId;
    private String pricingPlanName;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private Boolean hasMentoring;
    private Boolean hasAiCoaching;
    private Boolean hasGrowthReport;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private LocalDateTime createdAt;

    public static SubscriptionResponse from(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .userName(subscription.getUser().getName())
                .seasonId(subscription.getSeason().getId())
                .seasonTitle(subscription.getSeason().getTitle())
                .pricingPlanId(subscription.getPricingPlan().getId())
                .pricingPlanName(subscription.getPricingPlan().getName())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .isActive(subscription.isActive())
                .hasMentoring(subscription.hasMentoring())
                .hasAiCoaching(subscription.hasAiCoaching())
                .hasGrowthReport(subscription.hasGrowthReport())
                .cancelledAt(subscription.getCancelledAt())
                .cancellationReason(subscription.getCancellationReason())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
