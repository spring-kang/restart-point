package com.restartpoint.domain.payment.entity;

import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문/결제
 * 사용자의 결제 내역을 관리
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문 번호 (고유)
    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_plan_id", nullable = false)
    private PricingPlan pricingPlan;

    // 결제 금액
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // 결제 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    // 결제 수단 (CARD, TRANSFER 등)
    @Column(name = "payment_method")
    private String paymentMethod;

    // 외부 결제 시스템 거래 ID (토스페이먼츠, 스트라이프 등)
    @Column(name = "transaction_id")
    private String transactionId;

    // 결제 키 (토스페이먼츠)
    @Column(name = "payment_key")
    private String paymentKey;

    // 결제 완료 일시
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // 환불 일시
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    // 환불 사유
    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    // 메모
    @Column(length = 1000)
    private String notes;

    @Builder
    public Order(String orderNumber, User user, PricingPlan pricingPlan, BigDecimal amount) {
        this.orderNumber = orderNumber;
        this.user = user;
        this.pricingPlan = pricingPlan;
        this.amount = amount;
        this.status = OrderStatus.PENDING;
    }

    public void markPaid(String transactionId, String paymentKey, String paymentMethod) {
        this.status = OrderStatus.COMPLETED;
        this.transactionId = transactionId;
        this.paymentKey = paymentKey;
        this.paymentMethod = paymentMethod;
        this.paidAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = OrderStatus.FAILED;
        this.notes = reason;
    }

    public void markCancelled(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.notes = reason;
    }

    public void markRefunded(String reason) {
        this.status = OrderStatus.REFUNDED;
        this.refundReason = reason;
        this.refundedAt = LocalDateTime.now();
    }

    public enum OrderStatus {
        PENDING,     // 결제 대기
        COMPLETED,   // 결제 완료
        FAILED,      // 결제 실패
        CANCELLED,   // 결제 취소
        REFUNDED     // 환불 완료
    }
}
