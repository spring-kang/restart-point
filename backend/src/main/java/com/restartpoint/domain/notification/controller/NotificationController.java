package com.restartpoint.domain.notification.controller;

import com.restartpoint.domain.notification.dto.NotificationResponse;
import com.restartpoint.domain.notification.service.NotificationService;
import com.restartpoint.global.common.ApiResponse;
import com.restartpoint.global.security.CurrentUser;
import com.restartpoint.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @CurrentUser CustomUserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationResponse> notifications = notificationService.getNotifications(
                principal.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<NotificationResponse.UnreadCount>> getUnreadCount(
            @CurrentUser CustomUserPrincipal principal) {
        long count = notificationService.getUnreadCount(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(NotificationResponse.UnreadCount.of(count)));
    }

    /**
     * 알림 읽음 처리
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @CurrentUser CustomUserPrincipal principal) {
        int count = notificationService.markAllAsRead(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * 알림 삭제
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
