package com.restartpoint.domain.user.service;

import com.restartpoint.domain.user.dto.AuthResponse;
import com.restartpoint.domain.user.dto.LoginRequest;
import com.restartpoint.domain.user.dto.SignupRequest;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 이메일 인증 완료 여부 확인
        emailVerificationService.validateEmailVerified(request.getEmail());

        // 사용자 생성 (이메일 인증 완료 상태로)
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(Role.USER)
                .emailVerified(true)
                .build();

        User savedUser = userRepository.save(user);

        // 토큰 생성
        String token = jwtTokenProvider.createToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        return AuthResponse.of(token, savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 이메일 인증 여부 확인 (기존 미인증 사용자 차단)
        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 토큰 생성
        String token = jwtTokenProvider.createToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return AuthResponse.of(token, user);
    }
}
