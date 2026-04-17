package com.restartpoint.domain.payment.entity;

import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 가격 정책
 * 시즌별 참여 비용 정책을 정의
 */
@Entity
@Table(name = "pricing_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PricingPlan extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(nullable = false)
    private String name;  // 기본, 프리미엄, 엔터프라이즈

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // 원래 가격 (할인 전)
    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    // 할인율 (%)
    @Column(name = "discount_percentage")
    private Integer discountPercentage;

    // 포함 기능 (JSON 배열)
    @Column(length = 2000)
    private String features;

    // 멘토링 포함 여부
    @Column(name = "include_mentoring", nullable = false)
    private Boolean includeMentoring = false;

    // AI 코칭 포함 여부
    @Column(name = "include_ai_coaching", nullable = false)
    private Boolean includeAiCoaching = false;

    // 성장 리포트 포함 여부
    @Column(name = "include_growth_report", nullable = false)
    private Boolean includeGrowthReport = false;

    // 최대 팀 참여 수 (null이면 무제한)
    @Column(name = "max_team_participation")
    private Integer maxTeamParticipation;

    // 정렬 순서
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    // 활성화 여부
    @Column(nullable = false)
    private Boolean active = true;

    // 추천 플랜 여부
    @Column(nullable = false)
    private Boolean recommended = false;

    @Builder
    public PricingPlan(Season season, String name, String description, BigDecimal price,
                       BigDecimal originalPrice, Integer discountPercentage, String features,
                       Boolean includeMentoring, Boolean includeAiCoaching, Boolean includeGrowthReport,
                       Integer maxTeamParticipation, Integer displayOrder, Boolean recommended) {
        this.season = season;
        this.name = name;
        this.description = description;
        this.price = price;
        this.originalPrice = originalPrice;
        this.discountPercentage = discountPercentage;
        this.features = features;
        this.includeMentoring = includeMentoring != null ? includeMentoring : false;
        this.includeAiCoaching = includeAiCoaching != null ? includeAiCoaching : false;
        this.includeGrowthReport = includeGrowthReport != null ? includeGrowthReport : false;
        this.maxTeamParticipation = maxTeamParticipation;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.recommended = recommended != null ? recommended : false;
        this.active = true;
    }

    public void update(String name, String description, BigDecimal price,
                       BigDecimal originalPrice, Integer discountPercentage, String features,
                       Boolean includeMentoring, Boolean includeAiCoaching, Boolean includeGrowthReport,
                       Integer maxTeamParticipation, Integer displayOrder, Boolean recommended) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.originalPrice = originalPrice;
        this.discountPercentage = discountPercentage;
        this.features = features;
        this.includeMentoring = includeMentoring;
        this.includeAiCoaching = includeAiCoaching;
        this.includeGrowthReport = includeGrowthReport;
        this.maxTeamParticipation = maxTeamParticipation;
        this.displayOrder = displayOrder;
        this.recommended = recommended;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isFree() {
        return this.price.compareTo(BigDecimal.ZERO) == 0;
    }
}
