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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 시 이메일 인증 확인 후 비밀번호를 암호화하고 액세스 토큰을 반환한다")
    void signupSucceeds() {
        SignupRequest request = createSignupRequest("test@example.com", "password123", "테스터", "signup-token");
        given(userRepository.existsByEmail("test@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded-password");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            setField(savedUser, "id", 1L);
            return savedUser;
        });
        given(jwtTokenProvider.createToken(1L, "test@example.com", "USER")).willReturn("access-token");

        AuthResponse response = authService.signup(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser().getId()).isEqualTo(1L);
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUser().getRole()).isEqualTo(Role.USER);
        then(emailVerificationService).should()
                .validateAndConsumeSignupToken("test@example.com", "signup-token");
    }

    @Test
    @DisplayName("이미 사용 중인 이메일이면 회원가입에 실패한다")
    void signupFailsWhenEmailAlreadyExists() {
        SignupRequest request = createSignupRequest("duplicate@example.com", "password123", "중복유저", "signup-token");
        given(userRepository.existsByEmail("duplicate@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
                });
    }

    @Test
    @DisplayName("로그인 시 이메일과 비밀번호가 맞으면 액세스 토큰을 반환한다")
    void loginSucceeds() {
        LoginRequest request = createLoginRequest("test@example.com", "password123");
        User user = createUser(1L, "test@example.com", "encoded-password", "테스터");
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encoded-password")).willReturn(true);
        given(jwtTokenProvider.createToken(1L, "test@example.com", "USER")).willReturn("access-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser().getId()).isEqualTo(1L);
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 로그인에 실패한다")
    void loginFailsWhenUserNotFound() {
        LoginRequest request = createLoginRequest("missing@example.com", "password123");
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
    void loginFailsWhenPasswordIsInvalid() {
        LoginRequest request = createLoginRequest("test@example.com", "wrong-password");
        User user = createUser(1L, "test@example.com", "encoded-password", "테스터");
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD);
                });
    }

    private User createUser(Long id, String email, String password, String name) {
        User user = User.builder()
                .email(email)
                .password(password)
                .name(name)
                .role(Role.USER)
                .emailVerified(true)
                .build();
        setField(user, "id", id);
        return user;
    }

    private SignupRequest createSignupRequest(String email, String password, String name, String signupToken) {
        SignupRequest request = new SignupRequest();
        setField(request, "email", email);
        setField(request, "password", password);
        setField(request, "name", name);
        setField(request, "signupToken", signupToken);
        return request;
    }

    private LoginRequest createLoginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        setField(request, "email", email);
        setField(request, "password", password);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("테스트 필드 설정에 실패했습니다: " + fieldName, exception);
        }
    }
}
