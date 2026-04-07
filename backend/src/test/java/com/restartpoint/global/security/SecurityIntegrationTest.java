package com.restartpoint.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("공개 인증 API는 토큰 없이도 접근할 수 있다")
    void authEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "invalid-email",
                                "password", "password123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_002"));
    }

    @Test
    @DisplayName("보호된 API는 토큰이 없으면 접근할 수 없다")
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("유효한 사용자 토큰이 있으면 내 정보 조회가 가능하다")
    void authenticatedUserCanAccessProtectedEndpoint() throws Exception {
        User savedUser = userRepository.save(User.builder()
                .email("user@example.com")
                .password("encoded-password")
                .name("일반유저")
                .role(Role.USER)
                .build());
        String token = jwtTokenProvider.createToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    @Test
    @DisplayName("관리자 전용 API는 일반 사용자 토큰으로 접근할 수 없다")
    void adminEndpointRejectsNormalUser() throws Exception {
        User savedUser = userRepository.save(User.builder()
                .email("user@example.com")
                .password("encoded-password")
                .name("일반유저")
                .role(Role.USER)
                .build());
        String token = jwtTokenProvider.createToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());

        mockMvc.perform(get("/api/v1/admin/users/certifications/pending")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
