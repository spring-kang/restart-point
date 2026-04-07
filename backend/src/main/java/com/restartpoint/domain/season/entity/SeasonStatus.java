package com.restartpoint.domain.season.entity;

public enum SeasonStatus {
    DRAFT,          // 초안 (운영자만 볼 수 있음)
    RECRUITING,     // 모집 중
    TEAM_BUILDING,  // 팀빌딩 중
    IN_PROGRESS,    // 프로젝트 진행 중
    REVIEWING,      // 심사 중
    COMPLETED       // 완료
}
