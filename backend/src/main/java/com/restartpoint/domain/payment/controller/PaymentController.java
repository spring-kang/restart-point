package com.restartpoint.domain.payment.controller;

import com.restartpoint.domain.payment.dto.*;
import com.restartpoint.domain.payment.service.PaymentService;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Payment", description = "결제 및 구독 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ========== PricingPlan API ==========

    @Operation(summary = "가격 정책 생성", description = "시즌에 가격 정책을 추가합니다.")
    @PostMapping("/seasons/{seasonId}/pricing-plans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PricingPlanResponse> createPricingPlan(
            @PathVariable Long seasonId,
            @Valid @RequestBody PricingPlanRequest request) {
        return ResponseEntity.ok(paymentService.createPricingPlan(seasonId, request));
    }

    @Operation(summary = "시즌 가격 정책 목록", description = "특정 시즌의 가격 정책 목록을 조회합니다.")
    @GetMapping("/seasons/{seasonId}/pricing-plans")
    public ResponseEntity<List<PricingPlanResponse>> getPricingPlans(@PathVariable Long seasonId) {
        return ResponseEntity.ok(paymentService.getPricingPlansBySeasonId(seasonId));
    }

    @Operation(summary = "가격 정책 상세", description = "가격 정책의 상세 정보를 조회합니다.")
    @GetMapping("/pricing-plans/{planId}")
    public ResponseEntity<PricingPlanResponse> getPricingPlan(@PathVariable Long planId) {
        return ResponseEntity.ok(paymentService.getPricingPlan(planId));
    }

    @Operation(summary = "가격 정책 수정", description = "가격 정책을 수정합니다.")
    @PutMapping("/pricing-plans/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PricingPlanResponse> updatePricingPlan(
            @PathVariable Long planId,
            @Valid @RequestBody PricingPlanRequest request) {
        return ResponseEntity.ok(paymentService.updatePricingPlan(planId, request));
    }

    @Operation(summary = "가격 정책 삭제", description = "가격 정책을 비활성화합니다.")
    @DeleteMapping("/pricing-plans/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePricingPlan(@PathVariable Long planId) {
        paymentService.deletePricingPlan(planId);
        return ResponseEntity.noContent().build();
    }

    // ========== Order API ==========

    @Operation(summary = "주문 생성", description = "결제를 위한 주문을 생성합니다.")
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(paymentService.createOrder(userPrincipal.getUserId(), request));
    }

    @Operation(summary = "주문 상세 조회", description = "주문의 상세 정보를 조회합니다.")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getOrder(userPrincipal.getUserId(), orderId));
    }

    @Operation(summary = "주문번호로 조회", description = "주문번호로 주문을 조회합니다.")
    @GetMapping("/orders/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(paymentService.getOrderByOrderNumber(userPrincipal.getUserId(), orderNumber));
    }

    @Operation(summary = "내 주문 목록", description = "내 주문 목록을 조회합니다.")
    @GetMapping("/orders/me")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal) {
        return ResponseEntity.ok(paymentService.getMyOrders(userPrincipal.getUserId()));
    }

    @Operation(summary = "결제 승인", description = "결제를 승인 처리합니다. (토스페이먼츠 콜백)")
    @PostMapping("/payments/confirm")
    public ResponseEntity<OrderResponse> confirmPayment(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @Valid @RequestBody PaymentConfirmRequest request) {
        return ResponseEntity.ok(paymentService.confirmPayment(userPrincipal.getUserId(), request));
    }

    @Operation(summary = "주문 취소", description = "결제 전 주문을 취소합니다.")
    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(paymentService.cancelOrder(userPrincipal.getUserId(), orderId, reason));
    }

    @Operation(summary = "환불 요청", description = "결제 완료된 주문을 환불합니다.")
    @PostMapping("/orders/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> refundOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(paymentService.refundOrder(orderId, reason));
    }

    // ========== Subscription API ==========

    @Operation(summary = "내 구독 조회", description = "특정 시즌의 내 구독 정보를 조회합니다.")
    @GetMapping("/seasons/{seasonId}/subscription/me")
    public ResponseEntity<SubscriptionResponse> getMySubscription(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long seasonId) {
        return ResponseEntity.ok(paymentService.getMySubscription(userPrincipal.getUserId(), seasonId));
    }

    @Operation(summary = "내 전체 구독 목록", description = "내 모든 구독 목록을 조회합니다.")
    @GetMapping("/subscriptions/me")
    public ResponseEntity<List<SubscriptionResponse>> getMySubscriptions(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal) {
        return ResponseEntity.ok(paymentService.getMySubscriptions(userPrincipal.getUserId()));
    }

    @Operation(summary = "구독 상태 확인", description = "특정 시즌의 구독 상태를 확인합니다.")
    @GetMapping("/seasons/{seasonId}/subscription/check")
    public ResponseEntity<Boolean> hasActiveSubscription(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long seasonId) {
        return ResponseEntity.ok(paymentService.hasActiveSubscription(userPrincipal.getUserId(), seasonId));
    }

    @Operation(summary = "멘토링 접근 권한 확인", description = "멘토링 기능 접근 권한을 확인합니다.")
    @GetMapping("/seasons/{seasonId}/subscription/mentoring-access")
    public ResponseEntity<Boolean> hasMentoringAccess(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long seasonId) {
        return ResponseEntity.ok(paymentService.hasMentoringAccess(userPrincipal.getUserId(), seasonId));
    }

    @Operation(summary = "구독 취소", description = "구독을 취소합니다.")
    @PostMapping("/seasons/{seasonId}/subscription/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @Parameter(hidden = true) @CurrentUser CustomUserPrincipal userPrincipal,
            @PathVariable Long seasonId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(paymentService.cancelSubscription(
                userPrincipal.getUserId(), seasonId, reason));
    }
}
