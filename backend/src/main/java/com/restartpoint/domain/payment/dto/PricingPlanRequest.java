package com.restartpoint.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class PricingPlanRequest {

    @NotBlank(message = "플랜 이름은 필수입니다")
    private String name;

    private String description;

    @NotNull(message = "가격은 필수입니다")
    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer discountPercentage;

    private List<String> features;

    private Boolean includeMentoring;

    private Boolean includeAiCoaching;

    private Boolean includeGrowthReport;

    private Integer maxTeamParticipation;

    private Integer displayOrder;

    private Boolean recommended;
}
