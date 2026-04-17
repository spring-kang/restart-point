package com.restartpoint.domain.admin.controller;

import com.restartpoint.domain.admin.dto.AdminOrderResponse;
import com.restartpoint.domain.admin.dto.AdminSubscriptionResponse;
import com.restartpoint.domain.admin.dto.PaymentStatsResponse;
import com.restartpoint.domain.admin.service.AdminPaymentService;
import com.restartpoint.domain.payment.dto.OrderResponse;
import com.restartpoint.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Admin Payment", description = "관리자 결제 관리 API")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @Operation(summary = "시즌별 주문 목록 조회")
    @GetMapping("/seasons/{seasonId}/orders")
    public ResponseEntity<ApiResponse<List<AdminOrderResponse>>> getOrders(
            @PathVariable Long seasonId) {
        return ResponseEntity.ok(ApiResponse.success(adminPaymentService.getOrdersBySeasonId(seasonId)));
    }

    @Operation(summary = "주문 환불 처리")
    @PostMapping("/orders/{orderId}/refund")
    public ResponseEntity<ApiResponse<OrderResponse>> refundOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        return ResponseEntity.ok(ApiResponse.success(adminPaymentService.refundOrder(orderId, reason)));
    }

    @Operation(summary = "시즌별 구독 목록 조회")
    @GetMapping("/seasons/{seasonId}/subscriptions")
    public ResponseEntity<ApiResponse<List<AdminSubscriptionResponse>>> getSubscriptions(
            @PathVariable Long seasonId) {
        return ResponseEntity.ok(ApiResponse.success(adminPaymentService.getSubscriptionsBySeasonId(seasonId)));
    }

    @Operation(summary = "시즌별 결제 통계 조회")
    @GetMapping("/seasons/{seasonId}/payment-stats")
    public ResponseEntity<ApiResponse<PaymentStatsResponse>> getPaymentStats(
            @PathVariable Long seasonId) {
        return ResponseEntity.ok(ApiResponse.success(adminPaymentService.getPaymentStats(seasonId)));
    }
}
