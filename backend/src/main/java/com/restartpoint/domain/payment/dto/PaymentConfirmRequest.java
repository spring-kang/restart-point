package com.restartpoint.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 토스페이먼츠 결제 승인 요청
 */
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {

    @NotBlank(message = "결제 키는 필수입니다")
    private String paymentKey;

    @NotBlank(message = "주문 번호는 필수입니다")
    private String orderId;

    private BigDecimal amount;
}
