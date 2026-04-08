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
import org.springframework.context.ApplicationContext;
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
    private final ApplicationContext applicationContext;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 사용자 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        // 토큰 생성
        String token = jwtTokenProvider.createToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        // 이메일 인증 코드 발송 (트랜잭션 커밋 후 실행을 위해 ApplicationContext 사용)
        EmailVerificationService emailVerificationService =
                applicationContext.getBean(EmailVerificationService.class);
        emailVerificationService.sendVerificationCode(savedUser.getEmail());

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

        // 토큰 생성
        String token = jwtTokenProvider.createToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return AuthResponse.of(token, user);
    }
}
