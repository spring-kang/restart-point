package com.restartpoint.domain.notification.entity;

/**
 * 알림 유형
 */
public enum NotificationType {
    // 인증 관련
    CERTIFICATION_APPROVED,    // 수료 인증 승인
    CERTIFICATION_REJECTED,    // 수료 인증 반려

    // 팀 관련
    TEAM_INVITATION,           // 팀 초대 (지원 승인)
    TEAM_APPLICATION,          // 팀 지원 알림 (리더에게)
    TEAM_APPLICATION_REJECTED, // 팀 지원 거절

    // 영입 요청 관련
    TEAM_RECRUIT_REQUEST,      // 영입 요청 받음 (초대받은 사용자에게)
    TEAM_RECRUIT_ACCEPTED,     // 영입 요청 수락됨 (팀 리더에게)
    TEAM_RECRUIT_REJECTED,     // 영입 요청 거절됨 (팀 리더에게)

    // 마감 임박 리마인더
    CHECKPOINT_REMINDER,       // 체크포인트 마감 임박
    SUBMISSION_REMINDER,       // 최종 제출 마감 임박

    // 심사 관련
    REVIEW_START,              // 심사 시작
    REVIEW_END,                // 심사 종료

    // 리포트 관련
    REPORT_PUBLISHED,          // 성장 리포트 발행

    // 커뮤니티 관련
    COMMENT_ON_POST,           // 내 게시글에 댓글
    REPLY_ON_COMMENT           // 내 댓글에 답글
}
