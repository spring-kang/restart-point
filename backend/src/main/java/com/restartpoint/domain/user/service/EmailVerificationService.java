package com.restartpoint.domain.user.service;

import com.restartpoint.domain.user.entity.EmailVerification;
import com.restartpoint.domain.user.entity.User;
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

    // 인증 코드 발송
    @Transactional
    public void sendVerificationCode(String email) {
        // 이미 인증된 사용자인지 확인
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isEmailVerified()) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
            }
        });

        // 기존 미사용 코드 무효화
        emailVerificationRepository.invalidateAllByEmail(email);

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

    // 인증 코드 확인
    @Transactional
    public void verifyCode(String email, String code) {
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

        // 코드 사용 처리
        verification.markAsUsed();

        // 사용자 이메일 인증 완료 처리
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.verifyEmail();

        log.info("이메일 인증 완료: email={}", email);
    }

    // 인증 코드 재발송
    @Transactional
    public void resendVerificationCode(String email) {
        // 사용자 존재 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        sendVerificationCode(email);
    }

    private String generateVerificationCode() {
        int code = secureRandom.nextInt(900000) + 100000; // 100000 ~ 999999
        return String.valueOf(code);
    }
}
