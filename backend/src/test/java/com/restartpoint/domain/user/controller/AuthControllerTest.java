package com.restartpoint.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.user.dto.AuthResponse;
import com.restartpoint.domain.user.dto.UserResponse;
import com.restartpoint.domain.user.entity.CertificationStatus;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.service.AuthService;
import com.restartpoint.domain.user.service.EmailVerificationService;
import com.restartpoint.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("회원가입 요청이 성공하면 201과 액세스 토큰을 반환한다")
    void signupReturnsCreated() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .user(createUserResponse())
                .build();
        given(authService.signup(any())).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "test@example.com",
                                "password", "password123",
                                "name", "테스터"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다. 이메일 인증을 진행해주세요."))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("로그인 요청 본문이 잘못되면 400과 검증 메시지를 반환한다")
    void loginReturnsBadRequestForInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "invalid-email",
                                "password", "password123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_002"))
                .andExpect(jsonPath("$.message").value("email: 올바른 이메일 형식이 아닙니다."));
    }

    private UserResponse createUserResponse() {
        return UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스터")
                .role(Role.USER)
                .certificationStatus(CertificationStatus.NONE)
                .build();
    }
}
