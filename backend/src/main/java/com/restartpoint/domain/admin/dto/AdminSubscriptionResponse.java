package com.restartpoint.domain.admin.dto;

import com.restartpoint.domain.payment.entity.Subscription;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminSubscriptionResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String planName;
    private String seasonTitle;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean hasMentoring;

    public static AdminSubscriptionResponse from(Subscription subscription) {
        return AdminSubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .userName(subscription.getUser().getName())
                .userEmail(subscription.getUser().getEmail())
                .planName(subscription.getPricingPlan().getName())
                .seasonTitle(subscription.getSeason().getTitle())
                .status(subscription.getStatus().name())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .hasMentoring(subscription.hasMentoring())
                .build();
    }
}
