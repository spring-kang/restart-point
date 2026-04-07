package com.restartpoint.domain.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.profile.dto.ProfileResponse;
import com.restartpoint.domain.profile.entity.CollaborationStyle;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.profile.entity.ProjectDifficulty;
import com.restartpoint.domain.profile.service.ProfileService;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("내 프로필이 없으면 success 응답과 함께 data 필드 없이 반환한다")
    void getMyProfileReturnsNullWhenProfileDoesNotExist() throws Exception {
        given(profileService.getMyProfile(1L)).willReturn(Optional.empty());
        SecurityContextHolder.setContext(userSecurityContext());

        mockMvc.perform(get("/api/v1/users/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("프로필 저장 요청이 성공하면 저장된 프로필을 반환한다")
    void createOrUpdateProfileReturnsSavedProfile() throws Exception {
        given(profileService.createOrUpdateProfile(any(Long.class), any())).willReturn(createProfileResponse());
        SecurityContextHolder.setContext(userSecurityContext());

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobRole", "BACKEND",
                                "techStacks", List.of("Java", "Spring Boot"),
                                "portfolioUrl", "https://example.com/portfolio",
                                "interestedDomains", List.of("AI/ML"),
                                "availableHoursPerWeek", 20,
                                "collaborationStyle", "COLLABORATIVE",
                                "improvementGoal", "백엔드 아키텍처 설계",
                                "preferredDifficulty", "ADVANCED",
                                "introduction", "백엔드 개발자입니다."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("프로필이 저장되었습니다."))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.jobRole").value("BACKEND"));

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("프로필 상세 조회 시 프로필이 없으면 404를 반환한다")
    void getProfileReturnsNotFoundWhenProfileDoesNotExist() throws Exception {
        given(profileService.getProfile(99L))
                .willThrow(new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/profiles/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PROFILE_001"))
                .andExpect(jsonPath("$.message").value("프로필을 찾을 수 없습니다."));
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

    private ProfileResponse createProfileResponse() {
        return ProfileResponse.builder()
                .id(10L)
                .userId(1L)
                .userName("테스터")
                .jobRole(JobRole.BACKEND)
                .techStacks(List.of("Java", "Spring Boot"))
                .portfolioUrl("https://example.com/portfolio")
                .interestedDomains(List.of("AI/ML"))
                .availableHoursPerWeek(20)
                .collaborationStyle(CollaborationStyle.COLLABORATIVE)
                .improvementGoal("백엔드 아키텍처 설계")
                .preferredDifficulty(ProjectDifficulty.ADVANCED)
                .introduction("백엔드 개발자입니다.")
                .build();
    }
}
