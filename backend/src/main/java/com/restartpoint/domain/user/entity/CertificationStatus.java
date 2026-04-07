package com.restartpoint.domain.user.entity;

public enum CertificationStatus {
    NONE,       // 인증 요청 전
    PENDING,    // 인증 대기 중
    APPROVED,   // 인증 승인
    REJECTED    // 인증 거절
}
