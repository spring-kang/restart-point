package com.restartpoint.domain.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderRequest {

    @NotNull(message = "가격 정책 ID는 필수입니다")
    private Long pricingPlanId;
}
