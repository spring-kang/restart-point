package com.restartpoint.domain.payment.entity;

import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 구독/참여권
 * 사용자의 시즌별 참여 권한을 관리
 */
@Entity
@Table(name = "subscriptions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "season_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_plan_id", nullable = false)
    private PricingPlan pricingPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    // 시작일
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    // 종료일 (시즌 종료일과 동일하거나 환불 시 조기 종료)
    @Column(name = "end_date")
    private LocalDateTime endDate;

    // 취소일
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // 취소 사유
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Builder
    public Subscription(User user, Season season, PricingPlan pricingPlan, Order order,
                        LocalDateTime startDate, LocalDateTime endDate) {
        this.user = user;
        this.season = season;
        this.pricingPlan = pricingPlan;
        this.order = order;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = SubscriptionStatus.ACTIVE;
    }

    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
    }

    public void cancel(String reason) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }

    public boolean isActive() {
        if (status != SubscriptionStatus.ACTIVE) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startDate) && (endDate == null || now.isBefore(endDate));
    }

    public boolean hasMentoring() {
        return pricingPlan.getIncludeMentoring();
    }

    public boolean hasAiCoaching() {
        return pricingPlan.getIncludeAiCoaching();
    }

    public boolean hasGrowthReport() {
        return pricingPlan.getIncludeGrowthReport();
    }

    public enum SubscriptionStatus {
        ACTIVE,      // 활성
        EXPIRED,     // 만료
        CANCELLED    // 취소
    }
}
