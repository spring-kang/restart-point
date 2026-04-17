package com.restartpoint.domain.payment.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.payment.entity.PricingPlan;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class PricingPlanResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Long id;
    private Long seasonId;
    private String seasonTitle;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercentage;
    private List<String> features;
    private Boolean includeMentoring;
    private Boolean includeAiCoaching;
    private Boolean includeGrowthReport;
    private Integer maxTeamParticipation;
    private Integer displayOrder;
    private Boolean active;
    private Boolean recommended;
    private Boolean isFree;
    private LocalDateTime createdAt;

    public static PricingPlanResponse from(PricingPlan plan) {
        return PricingPlanResponse.builder()
                .id(plan.getId())
                .seasonId(plan.getSeason().getId())
                .seasonTitle(plan.getSeason().getTitle())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .originalPrice(plan.getOriginalPrice())
                .discountPercentage(plan.getDiscountPercentage())
                .features(parseJsonList(plan.getFeatures()))
                .includeMentoring(plan.getIncludeMentoring())
                .includeAiCoaching(plan.getIncludeAiCoaching())
                .includeGrowthReport(plan.getIncludeGrowthReport())
                .maxTeamParticipation(plan.getMaxTeamParticipation())
                .displayOrder(plan.getDisplayOrder())
                .active(plan.getActive())
                .recommended(plan.getRecommended())
                .isFree(plan.isFree())
                .createdAt(plan.getCreatedAt())
                .build();
    }

    private static List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
