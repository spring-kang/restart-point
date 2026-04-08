package com.restartpoint.domain.user.entity;

import com.restartpoint.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    // 인증 완료 여부 (회원가입 전까지 유효)
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean verified = false;

    // 인증 완료 후 회원가입 가능 시간 (30분)
    private LocalDateTime verifiedAt;

    @Column(length = 100)
    private String signupToken;

    private LocalDateTime signupTokenExpiresAt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean signupCompleted = false;

    @Builder
    public EmailVerification(String email, String code, int expirationMinutes) {
        this.email = email;
        this.code = code;
        this.expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid(String inputCode) {
        return !this.used && !isExpired() && this.code.equals(inputCode);
    }

    public void markAsUsed() {
        this.used = true;
    }

    public void markAsVerified(String signupToken, int signupWindowMinutes) {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
        this.used = true;
        this.signupToken = signupToken;
        this.signupTokenExpiresAt = this.verifiedAt.plusMinutes(signupWindowMinutes);
        this.signupCompleted = false;
    }

    public boolean hasActiveSignupToken() {
        if (!this.verified || this.signupCompleted || this.signupToken == null || this.signupTokenExpiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(this.signupTokenExpiresAt);
    }

    public boolean isSignupTokenExpired() {
        return this.signupTokenExpiresAt != null && LocalDateTime.now().isAfter(this.signupTokenExpiresAt);
    }

    public void completeSignup() {
        this.signupCompleted = true;
        this.signupToken = null;
        this.signupTokenExpiresAt = null;
    }

    public void invalidateForNewAttempt() {
        this.used = true;
        this.verified = false;
        this.verifiedAt = null;
        this.signupToken = null;
        this.signupTokenExpiresAt = null;
        this.signupCompleted = false;
    }
}
