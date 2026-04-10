package com.restartpoint.domain.notification.entity;

import com.restartpoint.domain.user.entity.User;
import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 알림 엔티티
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user_read", columnList = "user_id, is_read"),
    @Index(name = "idx_notification_user_created", columnList = "user_id, created_at DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String message;

    // 연관 리소스 정보 (선택적)
    @Column(name = "resource_type", length = 50)
    private String resourceType;  // TEAM, PROJECT, POST, SEASON 등

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /**
     * 알림 읽음 처리
     */
    public void markAsRead() {
        this.read = true;
    }
}
