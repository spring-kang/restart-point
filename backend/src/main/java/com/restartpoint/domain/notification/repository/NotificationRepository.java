package com.restartpoint.domain.notification.repository;

import com.restartpoint.domain.notification.entity.Notification;
import com.restartpoint.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 알림 레포지토리
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자의 알림 목록 조회 (최신순)
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자의 읽지 않은 알림 목록 조회
     */
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자의 읽지 않은 알림 개수
     */
    long countByUserIdAndReadFalse(Long userId);

    /**
     * 사용자의 특정 유형 알림 조회
     */
    List<Notification> findByUserIdAndNotificationTypeOrderByCreatedAtDesc(
            Long userId, NotificationType notificationType);

    /**
     * 사용자의 모든 알림 읽음 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * 특정 리소스 관련 알림 삭제 (리소스 삭제 시)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.resourceType = :resourceType AND n.resourceId = :resourceId")
    void deleteByResource(@Param("resourceType") String resourceType, @Param("resourceId") Long resourceId);

    /**
     * 사용자의 오래된 알림 삭제 (30일 이상)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId AND n.createdAt < CURRENT_TIMESTAMP - 30 DAY")
    void deleteOldNotifications(@Param("userId") Long userId);

    /**
     * 사용자의 모든 알림 삭제 (회원 삭제 시)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
