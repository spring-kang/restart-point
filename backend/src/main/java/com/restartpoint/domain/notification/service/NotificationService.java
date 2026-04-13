package com.restartpoint.domain.notification.service;

import com.restartpoint.domain.notification.dto.NotificationResponse;
import com.restartpoint.domain.notification.entity.Notification;
import com.restartpoint.domain.notification.entity.NotificationType;
import com.restartpoint.domain.notification.repository.NotificationRepository;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * 알림 생성
     */
    @Transactional
    public Notification createNotification(
            Long userId,
            NotificationType type,
            String title,
            String message,
            String resourceType,
            Long resourceId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.builder()
                .user(user)
                .notificationType(type)
                .title(title)
                .message(message)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("알림 생성: userId={}, type={}, title={}", userId, type, title);

        return saved;
    }

    /**
     * 알림 생성 (리소스 없음)
     */
    @Transactional
    public Notification createNotification(
            Long userId,
            NotificationType type,
            String title,
            String message
    ) {
        return createNotification(userId, type, title, message, null, null);
    }

    /**
     * 사용자의 알림 목록 조회
     */
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 본인의 알림인지 확인
        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        notification.markAsRead();
        log.info("알림 읽음 처리: notificationId={}", notificationId);
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        int count = notificationRepository.markAllAsReadByUserId(userId);
        log.info("모든 알림 읽음 처리: userId={}, count={}", userId, count);
        return count;
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 본인의 알림인지 확인
        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        notificationRepository.delete(notification);
        log.info("알림 삭제: notificationId={}", notificationId);
    }

    /**
     * 사용자의 모든 알림 삭제 (회원 삭제 시 사용)
     */
    @Transactional
    public void deleteAllByUserId(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
        log.info("사용자 알림 전체 삭제: userId={}", userId);
    }

    // ========== 알림 생성 헬퍼 메서드 ==========

    /**
     * 수료 인증 승인 알림
     */
    @Transactional
    public void notifyCertificationApproved(Long userId) {
        createNotification(
                userId,
                NotificationType.CERTIFICATION_APPROVED,
                "수료 인증이 승인되었습니다",
                "축하합니다! 수료 인증이 승인되어 프로젝트에 참여할 수 있습니다."
        );
    }

    /**
     * 수료 인증 반려 알림
     */
    @Transactional
    public void notifyCertificationRejected(Long userId, String reason) {
        createNotification(
                userId,
                NotificationType.CERTIFICATION_REJECTED,
                "수료 인증이 반려되었습니다",
                "반려 사유: " + (reason != null ? reason : "서류를 다시 확인해주세요.")
        );
    }

    /**
     * 팀 지원 승인 알림 (지원자에게)
     */
    @Transactional
    public void notifyTeamInvitation(Long userId, String teamName, Long teamId) {
        createNotification(
                userId,
                NotificationType.TEAM_INVITATION,
                "팀 가입이 승인되었습니다",
                "'" + teamName + "' 팀에 합류하셨습니다. 팀원들과 함께 프로젝트를 시작하세요!",
                "TEAM",
                teamId
        );
    }

    /**
     * 팀 지원 알림 (리더에게)
     */
    @Transactional
    public void notifyTeamApplication(Long leaderId, String applicantName, String teamName, Long teamId) {
        createNotification(
                leaderId,
                NotificationType.TEAM_APPLICATION,
                "새로운 팀 지원이 있습니다",
                "'" + applicantName + "'님이 '" + teamName + "' 팀에 지원했습니다.",
                "TEAM",
                teamId
        );
    }

    /**
     * 팀 지원 거절 알림
     */
    @Transactional
    public void notifyTeamApplicationRejected(Long userId, String teamName) {
        createNotification(
                userId,
                NotificationType.TEAM_APPLICATION_REJECTED,
                "팀 지원이 거절되었습니다",
                "'" + teamName + "' 팀 지원이 거절되었습니다. 다른 팀에 지원해보세요."
        );
    }

    /**
     * 체크포인트 마감 임박 알림
     */
    @Transactional
    public void notifyCheckpointReminder(Long userId, String projectName, int weekNumber, Long projectId) {
        createNotification(
                userId,
                NotificationType.CHECKPOINT_REMINDER,
                "체크포인트 마감이 임박했습니다",
                "'" + projectName + "' 프로젝트의 " + weekNumber + "주차 체크포인트 마감이 다가오고 있습니다.",
                "PROJECT",
                projectId
        );
    }

    /**
     * 최종 제출 마감 임박 알림
     */
    @Transactional
    public void notifySubmissionReminder(Long userId, String projectName, Long projectId) {
        createNotification(
                userId,
                NotificationType.SUBMISSION_REMINDER,
                "최종 제출 마감이 임박했습니다",
                "'" + projectName + "' 프로젝트의 최종 제출 마감이 다가오고 있습니다. 서둘러 제출해주세요!",
                "PROJECT",
                projectId
        );
    }

    /**
     * 심사 시작 알림
     */
    @Transactional
    public void notifyReviewStart(Long userId, String seasonTitle, Long seasonId) {
        createNotification(
                userId,
                NotificationType.REVIEW_START,
                "심사가 시작되었습니다",
                "'" + seasonTitle + "' 시즌의 프로젝트 심사가 시작되었습니다. 다른 팀의 프로젝트를 심사해주세요!",
                "SEASON",
                seasonId
        );
    }

    /**
     * 심사 종료 알림
     */
    @Transactional
    public void notifyReviewEnd(Long userId, String seasonTitle, Long seasonId) {
        createNotification(
                userId,
                NotificationType.REVIEW_END,
                "심사가 종료되었습니다",
                "'" + seasonTitle + "' 시즌의 프로젝트 심사가 종료되었습니다. 곧 결과가 발표됩니다.",
                "SEASON",
                seasonId
        );
    }

    /**
     * 성장 리포트 발행 알림
     */
    @Transactional
    public void notifyReportPublished(Long userId, String projectName, Long projectId) {
        createNotification(
                userId,
                NotificationType.REPORT_PUBLISHED,
                "성장 리포트가 발행되었습니다",
                "'" + projectName + "' 프로젝트의 성장 리포트가 준비되었습니다. 확인해보세요!",
                "PROJECT",
                projectId
        );
    }

    /**
     * 게시글 댓글 알림
     */
    @Transactional
    public void notifyCommentOnPost(Long authorId, String commenterName, String postTitle, Long postId) {
        createNotification(
                authorId,
                NotificationType.COMMENT_ON_POST,
                "새 댓글이 달렸습니다",
                "'" + commenterName + "'님이 '" + truncate(postTitle, 20) + "' 게시글에 댓글을 남겼습니다.",
                "POST",
                postId
        );
    }

    /**
     * 댓글 답글 알림
     */
    @Transactional
    public void notifyReplyOnComment(Long commentAuthorId, String replierName, Long postId) {
        createNotification(
                commentAuthorId,
                NotificationType.REPLY_ON_COMMENT,
                "새 답글이 달렸습니다",
                "'" + replierName + "'님이 회원님의 댓글에 답글을 남겼습니다.",
                "POST",
                postId
        );
    }

    /**
     * 영입 요청 받음 알림 (초대받은 사용자에게)
     */
    @Transactional
    public void notifyRecruitRequest(Long invitedUserId, String teamName, String leaderName, Long teamId) {
        createNotification(
                invitedUserId,
                NotificationType.TEAM_RECRUIT_REQUEST,
                "팀 영입 요청을 받았습니다",
                "'" + teamName + "' 팀의 '" + leaderName + "'님이 영입 요청을 보냈습니다. 확인해보세요!",
                "TEAM",
                teamId
        );
    }

    /**
     * 영입 요청 수락됨 알림 (팀 리더에게)
     */
    @Transactional
    public void notifyRecruitAccepted(Long leaderId, String acceptedUserName, String teamName, Long teamId) {
        createNotification(
                leaderId,
                NotificationType.TEAM_RECRUIT_ACCEPTED,
                "영입 요청이 수락되었습니다",
                "'" + acceptedUserName + "'님이 '" + teamName + "' 팀 영입 요청을 수락했습니다!",
                "TEAM",
                teamId
        );
    }

    /**
     * 영입 요청 거절됨 알림 (팀 리더에게)
     */
    @Transactional
    public void notifyRecruitRejected(Long leaderId, String rejectedUserName, String teamName) {
        createNotification(
                leaderId,
                NotificationType.TEAM_RECRUIT_REJECTED,
                "영입 요청이 거절되었습니다",
                "'" + rejectedUserName + "'님이 '" + teamName + "' 팀 영입 요청을 거절했습니다."
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
