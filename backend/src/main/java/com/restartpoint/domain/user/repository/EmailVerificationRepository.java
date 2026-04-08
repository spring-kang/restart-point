package com.restartpoint.domain.user.repository;

import com.restartpoint.domain.user.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    // 가장 최근 유효한 인증 코드 조회
    Optional<EmailVerification> findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, LocalDateTime now);

    // 이메일로 사용되지 않은 코드 모두 만료 처리
    @Modifying
    @Query("UPDATE EmailVerification e SET e.used = true WHERE e.email = :email AND e.used = false")
    void invalidateAllByEmail(@Param("email") String email);

    // 만료된 코드 삭제 (스케줄러용)
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);

    // 인증 완료된 이메일 조회 (회원가입용)
    Optional<EmailVerification> findTopByEmailAndVerifiedTrueOrderByVerifiedAtDesc(String email);
}
