package com.restartpoint.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.user.dto.UserResponse;
import com.restartpoint.domain.user.entity.CertificationStatus;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.service.UserService;
import com.restartpoint.global.security.CustomUserPrincipal;
import com.restartpoint.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({UserController.class, AdminUserController.class})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("내 정보 조회 시 현재 사용자 기준으로 응답을 반환한다")
    void getMeReturnsCurrentUser() throws Exception {
        given(userService.getMe(1L)).willReturn(createUserResponse(CertificationStatus.PENDING));
        SecurityContextHolder.setContext(userSecurityContext());

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.certificationStatus").value("PENDING"));

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("수료 인증 요청 본문이 잘못되면 400을 반환한다")
    void requestCertificationReturnsBadRequestForInvalidInput() throws Exception {
        SecurityContextHolder.setContext(userSecurityContext());

        mockMvc.perform(post("/api/v1/users/me/certification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bootcampName", "",
                                "bootcampGeneration", "1기",
                                "graduationDate", "2026-04-01",
                                "certificateUrl", "https://example.com/certificate"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_002"))
                .andExpect(jsonPath("$.message").value("bootcampName: 부트캠프명은 필수입니다."));

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("관리자 승인 요청이 성공하면 승인된 사용자 응답을 반환한다")
    void approveCertificationReturnsApprovedUser() throws Exception {
        given(userService.approveCertification(1L)).willReturn(createUserResponse(CertificationStatus.APPROVED));

        mockMvc.perform(post("/api/v1/admin/users/1/certification/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("수료 인증이 승인되었습니다."))
                .andExpect(jsonPath("$.data.certificationStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("인증 대기 목록 조회 시 목록을 반환한다")
    void getPendingCertificationsReturnsList() throws Exception {
        given(userService.getPendingCertifications()).willReturn(List.of(createUserResponse(CertificationStatus.PENDING)));

        mockMvc.perform(get("/api/v1/admin/users/certifications/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].certificationStatus").value("PENDING"));
    }

    private SecurityContext userSecurityContext() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(userAuthentication());
        return context;
    }

    private UsernamePasswordAuthenticationToken userAuthentication() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "test@example.com", "USER");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private UserResponse createUserResponse(CertificationStatus certificationStatus) {
        return UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스터")
                .role(Role.USER)
                .certificationStatus(certificationStatus)
                .bootcampName("코드잇 스프린트")
                .bootcampGeneration("1기")
                .graduationDate("2026-04-01")
                .build();
    }
}
