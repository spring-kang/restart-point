package com.restartpoint.domain.user.controller;

import com.restartpoint.domain.user.dto.AuthResponse;
import com.restartpoint.domain.user.dto.EmailVerificationConfirmRequest;
import com.restartpoint.domain.user.dto.EmailVerificationRequest;
import com.restartpoint.domain.user.dto.LoginRequest;
import com.restartpoint.domain.user.dto.SignupRequest;
import com.restartpoint.domain.user.service.AuthService;
import com.restartpoint.domain.user.service.EmailVerificationService;
import com.restartpoint.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "로그인되었습니다."));
    }

    // 이메일 인증 코드 발송
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @Valid @RequestBody EmailVerificationRequest request) {
        emailVerificationService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "인증 코드가 발송되었습니다."));
    }

    // 이메일 인증 코드 확인
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody EmailVerificationConfirmRequest request) {
        emailVerificationService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(null, "이메일 인증이 완료되었습니다. 회원가입을 진행해주세요."));
    }

    // 이메일 인증 상태 확인
    @PostMapping("/email/status")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailVerificationStatus(
            @Valid @RequestBody EmailVerificationRequest request) {
        boolean verified = emailVerificationService.isEmailVerified(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(verified, verified ? "인증된 이메일입니다." : "인증되지 않은 이메일입니다."));
    }

    // 이메일 인증 코드 재발송
    @PostMapping("/email/resend")
    public ResponseEntity<ApiResponse<Void>> resendVerificationCode(
            @Valid @RequestBody EmailVerificationRequest request) {
        emailVerificationService.resendVerificationCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "인증 코드가 재발송되었습니다."));
    }
}
