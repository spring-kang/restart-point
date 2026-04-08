package com.restartpoint.domain.user.repository;

import com.restartpoint.domain.user.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    // 가장 최근 유효한 인증 코드 조회
    Optional<EmailVerification> findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, LocalDateTime now);

    List<EmailVerification> findAllByEmailOrderByCreatedAtDesc(String email);

    // 만료된 코드 삭제 (스케줄러용)
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);

    // 인증 완료된 회원가입 세션 조회
    Optional<EmailVerification> findTopByEmailAndSignupTokenOrderByVerifiedAtDesc(String email, String signupToken);
}
