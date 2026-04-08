package com.restartpoint.domain.matching.controller;

import com.restartpoint.domain.matching.dto.MemberRecommendationResponse;
import com.restartpoint.domain.matching.dto.TeamRecommendationResponse;
import com.restartpoint.domain.matching.service.TeamMatchingService;
import com.restartpoint.domain.profile.dto.ProfileResponse;
import com.restartpoint.domain.profile.entity.CollaborationStyle;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.profile.entity.ProjectDifficulty;
import com.restartpoint.domain.team.dto.TeamResponse;
import com.restartpoint.domain.team.entity.TeamStatus;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamMatchingController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeamMatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamMatchingService teamMatchingService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("팀 추천 요청이 성공하면 추천 목록을 반환한다")
    void recommendTeamsReturnsRecommendations() throws Exception {
        List<TeamRecommendationResponse> recommendations = List.of(
                createTeamRecommendation(1L, "팀A", 85),
                createTeamRecommendation(2L, "팀B", 72)
        );
        given(teamMatchingService.recommendTeamsForUser(eq(1L), eq(1L), eq(5)))
                .willReturn(recommendations);
        SecurityContextHolder.setContext(userSecurityContext());

        mockMvc.perform(get("/api/v1/matching/teams")
                        .param("seasonId", "1")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].team.name").value("팀A"))
                .andExpect(jsonPath("$.data[0].matchScore").value(85))
                .andExpect(jsonPath("$.data[1].team.name").value("팀B"))
                .andExpect(jsonPath("$.data[1].matchScore").value(72));

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("프로필이 없으면 404 에러를 반환한다")
    void recommendTeamsReturnsNotFoundWhenProfileNotFound() throws Exception {
        given(teamMatchingService.recommendTeamsForUser(eq(1L), eq(1L), eq(5)))
                .willThrow(new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
        SecurityContextHolder.setContext(userSecurityContext());

        mockMvc.perform(get("/api/v1/matching/teams")
                        .param("seasonId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PROFILE_001"));

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("이미 팀에 소속된 경우 409 에러를 반환한다")
    void recommendTeamsReturnsConflictWhenAlreadyInTeam() throws Exception {
        given(teamMatchingService.recommendTeamsForUser(eq(1L), eq(1L), eq(5)))
                .willThrow(new BusinessException(ErrorCode.ALREADY_IN_TEAM));
        SecurityContextHolder.setContext(userSecurityContext());

        mockMvc.perform(get("/api/v1/matching/teams")
                        .param("seasonId", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TEAM_003"));

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("멤버 추천 요청이 성공하면 추천 목록을 반환한다")
    void recommendMembersReturnsRecommendations() throws Exception {
        List<MemberRecommendationResponse> recommendations = List.of(
                createMemberRecommendation(1L, "유저A", JobRole.BACKEND, 90),
                createMemberRecommendation(2L, "유저B", JobRole.FRONTEND, 78)
        );
        given(teamMatchingService.recommendMembersForTeam(eq(1L), eq(1L), eq(5)))
                .willReturn(recommendations);
        SecurityContextHolder.setContext(userSecurityContext());

        mockMvc.perform(get("/api/v1/matching/teams/1/members")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].profile.userName").value("유저A"))
                .andExpect(jsonPath("$.data[0].matchScore").value(90));

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("팀 리더가 아닌 경우 403 에러를 반환한다")
    void recommendMembersReturnsForbiddenWhenNotLeader() throws Exception {
        given(teamMatchingService.recommendMembersForTeam(eq(1L), eq(1L), eq(5)))
                .willThrow(new BusinessException(ErrorCode.NOT_TEAM_LEADER));
        SecurityContextHolder.setContext(userSecurityContext());

        mockMvc.perform(get("/api/v1/matching/teams/1/members"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TEAM_004"));

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("추천 가능한 후보가 없으면 404 에러를 반환한다")
    void recommendTeamsReturnsNotFoundWhenNoCandidate() throws Exception {
        given(teamMatchingService.recommendTeamsForUser(eq(1L), eq(1L), eq(5)))
                .willThrow(new BusinessException(ErrorCode.NO_MATCHING_CANDIDATES));
        SecurityContextHolder.setContext(userSecurityContext());

        mockMvc.perform(get("/api/v1/matching/teams")
                        .param("seasonId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AI_002"));

        SecurityContextHolder.clearContext();
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

    private TeamRecommendationResponse createTeamRecommendation(Long teamId, String teamName, int score) {
        TeamResponse team = TeamResponse.builder()
                .id(teamId)
                .name(teamName)
                .description("테스트 팀 설명")
                .seasonId(1L)
                .seasonTitle("시즌 1")
                .status(TeamStatus.RECRUITING)
                .leaderId(10L)
                .leaderName("리더")
                .recruitingBackend(true)
                .memberCount(2)
                .maxMemberCount(4)
                .build();

        return TeamRecommendationResponse.builder()
                .team(team)
                .matchScore(score)
                .reasons(List.of("이유1", "이유2", "이유3"))
                .balanceAnalysis("팀 밸런스 분석")
                .scheduleRisk("LOW")
                .missingRoles(List.of("UXUI"))
                .build();
    }

    private MemberRecommendationResponse createMemberRecommendation(Long profileId, String userName, JobRole role, int score) {
        ProfileResponse profile = ProfileResponse.builder()
                .id(profileId)
                .userId(profileId)
                .userName(userName)
                .jobRole(role)
                .techStacks(List.of("Java", "Spring"))
                .interestedDomains(List.of("AI/ML"))
                .availableHoursPerWeek(20)
                .collaborationStyle(CollaborationStyle.COLLABORATIVE)
                .preferredDifficulty(ProjectDifficulty.INTERMEDIATE)
                .build();

        return MemberRecommendationResponse.builder()
                .profile(profile)
                .matchScore(score)
                .reasons(List.of("이유1", "이유2", "이유3"))
                .balanceAnalysis("팀 밸런스에 긍정적")
                .scheduleRisk("LOW")
                .complementarySkills(List.of("Java", "Spring"))
                .build();
    }
}
