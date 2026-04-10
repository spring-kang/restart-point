package com.restartpoint.domain.notification.dto;

import com.restartpoint.domain.notification.entity.Notification;
import com.restartpoint.domain.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO
 */
@Getter
@Builder
public class NotificationResponse {

    private Long id;
    private NotificationType notificationType;
    private String title;
    private String message;
    private String resourceType;
    private Long resourceId;
    private boolean read;
    private LocalDateTime createdAt;

    /**
     * 엔티티를 DTO로 변환
     */
    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .notificationType(notification.getNotificationType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .resourceType(notification.getResourceType())
                .resourceId(notification.getResourceId())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    /**
     * 읽지 않은 알림 개수 응답
     */
    @Getter
    @Builder
    public static class UnreadCount {
        private long count;

        public static UnreadCount of(long count) {
            return UnreadCount.builder().count(count).build();
        }
    }
}
