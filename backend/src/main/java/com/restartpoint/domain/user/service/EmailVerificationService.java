package com.restartpoint.domain.user.service;

import com.restartpoint.domain.user.entity.EmailVerification;
import com.restartpoint.domain.user.dto.EmailVerificationResponse;
import com.restartpoint.domain.user.repository.EmailVerificationRepository;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.infra.mail.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${email.verification.expiration-minutes:10}")
    private int expirationMinutes;

    @Value("${email.verification.signup-window-minutes:30}")
    private int signupWindowMinutes;

    // 인증 코드 발송 (회원가입 전)
    @Transactional
    public void sendVerificationCode(String email) {
        // 이미 가입된 이메일인지 확인
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 기존 미사용 코드 무효화
        invalidateExistingAttempts(email);

        // 6자리 인증 코드 생성
        String code = generateVerificationCode();

        // 인증 코드 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .expirationMinutes(expirationMinutes)
                .build();
        emailVerificationRepository.save(verification);

        // 이메일 발송 (비동기)
        emailService.sendVerificationCode(email, code);

        log.info("인증 코드 발송: email={}", email);
    }

    // 인증 코드 확인 (회원가입 전)
    @Transactional
    public EmailVerificationResponse verifyCode(String email, String code) {
        // 유효한 인증 코드 조회
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(email, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_CODE_NOT_FOUND));

        // 코드 검증
        if (!verification.isValid(code)) {
            if (verification.isExpired()) {
                throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
            }
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 인증 완료 처리 (회원가입 가능 상태로 변경)
        verification.markAsVerified(generateSignupToken(), signupWindowMinutes);

        log.info("이메일 인증 완료: email={}", email);
        return new EmailVerificationResponse(verification.getSignupToken());
    }

    // 인증 코드 재발송
    @Transactional
    public void resendVerificationCode(String email) {
        // 이미 가입된 이메일인지 확인
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        sendVerificationCode(email);
    }

    // 이메일 인증 완료 여부 확인 (회원가입 시 호출)
    public boolean isEmailVerified(String email) {
        return emailVerificationRepository
                .findAllByEmailOrderByCreatedAtDesc(email)
                .stream()
                .anyMatch(EmailVerification::hasActiveSignupToken);
    }

    @Transactional
    public void validateAndConsumeSignupToken(String email, String signupToken) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailAndSignupTokenOrderByVerifiedAtDesc(email, signupToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.SIGNUP_TOKEN_NOT_FOUND));

        if (!verification.hasActiveSignupToken()) {
            if (verification.isSignupTokenExpired()) {
                throw new BusinessException(ErrorCode.SIGNUP_TOKEN_EXPIRED);
            }
            throw new BusinessException(ErrorCode.SIGNUP_TOKEN_NOT_FOUND);
        }

        verification.completeSignup();
    }

    private String generateVerificationCode() {
        int code = secureRandom.nextInt(900000) + 100000; // 100000 ~ 999999
        return String.valueOf(code);
    }

    private String generateSignupToken() {
        return UUID.randomUUID().toString();
    }

    private void invalidateExistingAttempts(String email) {
        for (EmailVerification verification : emailVerificationRepository.findAllByEmailOrderByCreatedAtDesc(email)) {
            verification.invalidateForNewAttempt();
        }
    }
}
