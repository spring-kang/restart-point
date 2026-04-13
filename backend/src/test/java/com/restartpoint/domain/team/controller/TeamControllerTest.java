package com.restartpoint.domain.team.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.team.dto.TeamApplyRequest;
import com.restartpoint.domain.team.dto.TeamMemberResponse;
import com.restartpoint.domain.team.dto.TeamRequest;
import com.restartpoint.domain.team.dto.TeamResponse;
import com.restartpoint.domain.team.entity.TeamMemberStatus;
import com.restartpoint.domain.team.entity.TeamStatus;
import com.restartpoint.domain.team.service.TeamService;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.global.security.CustomUserPrincipal;
import com.restartpoint.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeamControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private TeamService teamService;

  @MockBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthentication(Long userId, String email, String role) {
    CustomUserPrincipal principal = new CustomUserPrincipal(userId, email, role);
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
    );
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @Nested
  @DisplayName("팀 생성 API")
  class CreateTeamApi {

    @Test
    @DisplayName("인증된 사용자가 팀을 생성하면 성공한다")
    void createTeamSuccess() throws Exception {
      // given
      setAuthentication(1L, "user@test.com", "USER");
      TeamRequest request = createTeamRequest();
      TeamResponse response = createTeamResponse(1L, "테스트팀");

      given(teamService.createTeam(eq(1L), any(TeamRequest.class))).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/v1/teams")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.id").value(1))
          .andExpect(jsonPath("$.data.name").value("테스트팀"))
          .andExpect(jsonPath("$.message").value("팀이 생성되었습니다."));
    }

    @Test
    @DisplayName("수료 인증되지 않은 사용자가 팀을 생성하면 실패한다")
    void createTeamFailsWhenNotCertified() throws Exception {
      // given
      setAuthentication(1L, "user@test.com", "USER");
      TeamRequest request = createTeamRequest();

      given(teamService.createTeam(eq(1L), any(TeamRequest.class)))
          .willThrow(new BusinessException(ErrorCode.CERTIFICATION_REQUIRED));

      // when & then
      mockMvc.perform(post("/api/v1/teams")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.errorCode").value("USER_004"));
    }
  }

  @Nested
  @DisplayName("팀 조회 API")
  class GetTeamApi {

    @Test
    @DisplayName("팀 상세 조회에 성공한다")
    void getTeamSuccess() throws Exception {
      // given
      TeamResponse response = createTeamResponse(1L, "테스트팀");
      given(teamService.getTeam(1L)).willReturn(response);

      // when & then
      mockMvc.perform(get("/api/v1/teams/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.id").value(1))
          .andExpect(jsonPath("$.data.name").value("테스트팀"));
    }

    @Test
    @DisplayName("존재하지 않는 팀 조회 시 404를 반환한다")
    void getTeamFailsWhenNotFound() throws Exception {
      // given
      given(teamService.getTeam(999L))
          .willThrow(new BusinessException(ErrorCode.TEAM_NOT_FOUND));

      // when & then
      mockMvc.perform(get("/api/v1/teams/999"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.errorCode").value("TEAM_001"));
    }

    @Test
    @DisplayName("시즌별 팀 목록 조회에 성공한다")
    void getTeamsBySeasonSuccess() throws Exception {
      // given
      List<TeamResponse> teams = List.of(
          createTeamResponse(1L, "팀1"),
          createTeamResponse(2L, "팀2")
      );
      given(teamService.getTeamsBySeason(1L)).willReturn(teams);

      // when & then
      mockMvc.perform(get("/api/v1/seasons/1/teams"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(2))
          .andExpect(jsonPath("$.data[0].name").value("팀1"))
          .andExpect(jsonPath("$.data[1].name").value("팀2"));
    }

    @Test
    @DisplayName("시즌별 모집 중인 팀 목록 조회에 성공한다")
    void getRecruitingTeamsSuccess() throws Exception {
      // given
      List<TeamResponse> teams = List.of(createTeamResponse(1L, "모집중팀"));
      given(teamService.getRecruitingTeamsBySeason(1L)).willReturn(teams);

      // when & then
      mockMvc.perform(get("/api/v1/seasons/1/teams/recruiting"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(1))
          .andExpect(jsonPath("$.data[0].name").value("모집중팀"));
    }

    @Test
    @DisplayName("내가 리더인 팀 목록 조회에 성공한다")
    void getMyTeamsSuccess() throws Exception {
      // given
      setAuthentication(1L, "user@test.com", "USER");
      List<TeamResponse> teams = List.of(createTeamResponse(1L, "내팀"));
      given(teamService.getMyTeams(1L)).willReturn(teams);

      // when & then
      mockMvc.perform(get("/api/v1/users/me/teams"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(1))
          .andExpect(jsonPath("$.data[0].name").value("내팀"));
    }
  }

  @Nested
  @DisplayName("팀 지원 API")
  class ApplyToTeamApi {

    @Test
    @DisplayName("팀 지원에 성공한다")
    void applyToTeamSuccess() throws Exception {
      // given
      setAuthentication(2L, "applicant@test.com", "USER");
      TeamApplyRequest request = createApplyRequest();
      TeamMemberResponse response = createTeamMemberResponse(1L, 2L, JobRole.BACKEND, TeamMemberStatus.PENDING);

      given(teamService.applyToTeam(eq(2L), eq(1L), any(TeamApplyRequest.class))).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/v1/teams/1/applications")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.role").value("BACKEND"))
          .andExpect(jsonPath("$.data.status").value("PENDING"))
          .andExpect(jsonPath("$.message").value("팀에 지원했습니다."));
    }

    @Test
    @DisplayName("이미 지원한 팀에 중복 지원하면 실패한다")
    void applyToTeamFailsWhenAlreadyApplied() throws Exception {
      // given
      setAuthentication(2L, "applicant@test.com", "USER");
      TeamApplyRequest request = createApplyRequest();

      given(teamService.applyToTeam(eq(2L), eq(1L), any(TeamApplyRequest.class)))
          .willThrow(new BusinessException(ErrorCode.ALREADY_APPLIED));

      // when & then
      mockMvc.perform(post("/api/v1/teams/1/applications")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.errorCode").value("TEAM_005"));
    }
  }

  @Nested
  @DisplayName("팀 지원 수락/거절 API")
  class AcceptRejectApplicationApi {

    @Test
    @DisplayName("팀 리더가 지원을 수락하면 성공한다")
    void acceptApplicationSuccess() throws Exception {
      // given
      setAuthentication(1L, "leader@test.com", "USER");
      TeamMemberResponse response = createTeamMemberResponse(1L, 2L, JobRole.BACKEND, TeamMemberStatus.ACCEPTED);

      given(teamService.acceptApplication(1L, 1L, 1L)).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/v1/teams/1/applications/1/accept"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
          .andExpect(jsonPath("$.message").value("지원을 수락했습니다."));
    }

    @Test
    @DisplayName("팀 리더가 아닌 사용자가 지원을 수락하면 실패한다")
    void acceptApplicationFailsWhenNotLeader() throws Exception {
      // given
      setAuthentication(3L, "other@test.com", "USER");

      given(teamService.acceptApplication(3L, 1L, 1L))
          .willThrow(new BusinessException(ErrorCode.NOT_TEAM_LEADER));

      // when & then
      mockMvc.perform(post("/api/v1/teams/1/applications/1/accept"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.errorCode").value("TEAM_004"));
    }

    @Test
    @DisplayName("팀 리더가 지원을 거절하면 성공한다")
    void rejectApplicationSuccess() throws Exception {
      // given
      setAuthentication(1L, "leader@test.com", "USER");
      TeamMemberResponse response = createTeamMemberResponse(1L, 2L, JobRole.BACKEND, TeamMemberStatus.REJECTED);

      given(teamService.rejectApplication(1L, 1L, 1L)).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/v1/teams/1/applications/1/reject"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.status").value("REJECTED"))
          .andExpect(jsonPath("$.message").value("지원을 거절했습니다."));
    }
  }

  @Nested
  @DisplayName("팀 멤버 조회 API")
  class GetTeamMembersApi {

    @Test
    @DisplayName("팀 멤버 목록 조회에 성공한다")
    void getTeamMembersSuccess() throws Exception {
      // given
      List<TeamMemberResponse> members = List.of(
          createTeamMemberResponse(1L, 1L, JobRole.BACKEND, TeamMemberStatus.ACCEPTED),
          createTeamMemberResponse(2L, 2L, JobRole.FRONTEND, TeamMemberStatus.ACCEPTED)
      );
      given(teamService.getTeamMembers(1L)).willReturn(members);

      // when & then
      mockMvc.perform(get("/api/v1/teams/1/members"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("팀 지원 목록 조회에 성공한다 (리더)")
    void getTeamApplicationsSuccess() throws Exception {
      // given
      setAuthentication(1L, "leader@test.com", "USER");
      List<TeamMemberResponse> applications = List.of(
          createTeamMemberResponse(1L, 2L, JobRole.BACKEND, TeamMemberStatus.PENDING)
      );
      given(teamService.getTeamApplications(1L, 1L)).willReturn(applications);

      // when & then
      mockMvc.perform(get("/api/v1/teams/1/applications"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(1))
          .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }
  }

  @Nested
  @DisplayName("팀 탈퇴 API")
  class LeaveTeamApi {

    @Test
    @DisplayName("팀 멤버가 팀에서 탈퇴하면 성공한다")
    void leaveTeamSuccess() throws Exception {
      // given
      setAuthentication(2L, "member@test.com", "USER");
      doNothing().when(teamService).leaveTeam(2L, 1L);

      // when & then
      mockMvc.perform(delete("/api/v1/teams/1/members/me"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("팀에서 탈퇴했습니다."));

      verify(teamService).leaveTeam(2L, 1L);
    }

    @Test
    @DisplayName("팀 리더가 탈퇴하려고 하면 실패한다")
    void leaveTeamFailsWhenLeader() throws Exception {
      // given
      setAuthentication(1L, "leader@test.com", "USER");
      doThrow(new BusinessException(ErrorCode.NOT_TEAM_LEADER, "팀 리더는 탈퇴할 수 없습니다. 팀을 해체하거나 리더를 위임하세요."))
          .when(teamService).leaveTeam(1L, 1L);

      // when & then
      mockMvc.perform(delete("/api/v1/teams/1/members/me"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false));
    }
  }

  @Nested
  @DisplayName("내 지원 현황 조회 API")
  class GetMyApplicationsApi {

    @Test
    @DisplayName("내 지원 현황 조회에 성공한다")
    void getMyApplicationsSuccess() throws Exception {
      // given
      setAuthentication(2L, "user@test.com", "USER");
      List<TeamMemberResponse> applications = List.of(
          createTeamMemberResponse(1L, 2L, JobRole.BACKEND, TeamMemberStatus.PENDING)
      );
      given(teamService.getMyApplications(2L)).willReturn(applications);

      // when & then
      mockMvc.perform(get("/api/v1/users/me/applications"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(1));
    }
  }

  // 헬퍼 메서드
  private TeamRequest createTeamRequest() {
    TeamRequest request = new TeamRequest();
    try {
      var field = TeamRequest.class.getDeclaredField("seasonId");
      field.setAccessible(true);
      field.set(request, 1L);

      field = TeamRequest.class.getDeclaredField("name");
      field.setAccessible(true);
      field.set(request, "테스트팀");

      field = TeamRequest.class.getDeclaredField("description");
      field.setAccessible(true);
      field.set(request, "팀 설명");

      field = TeamRequest.class.getDeclaredField("leaderRole");
      field.setAccessible(true);
      field.set(request, JobRole.BACKEND);

      field = TeamRequest.class.getDeclaredField("recruitingBackend");
      field.setAccessible(true);
      field.set(request, true);

      field = TeamRequest.class.getDeclaredField("recruitingFrontend");
      field.setAccessible(true);
      field.set(request, true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return request;
  }

  private TeamApplyRequest createApplyRequest() {
    TeamApplyRequest request = new TeamApplyRequest();
    try {
      var field = TeamApplyRequest.class.getDeclaredField("role");
      field.setAccessible(true);
      field.set(request, JobRole.BACKEND);

      field = TeamApplyRequest.class.getDeclaredField("applicationMessage");
      field.setAccessible(true);
      field.set(request, "지원합니다");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return request;
  }

  private TeamResponse createTeamResponse(Long id, String name) {
    return TeamResponse.builder()
        .id(id)
        .name(name)
        .description("팀 설명")
        .seasonId(1L)
        .seasonTitle("시즌1")
        .leaderId(1L)
        .leaderName("팀장")
        .status(TeamStatus.RECRUITING)
        .memberCount(1)
        .recruitingPlanner(false)
        .recruitingUxui(false)
        .recruitingFrontend(true)
        .recruitingBackend(true)
        .createdAt(LocalDateTime.now())
        .build();
  }

  private TeamMemberResponse createTeamMemberResponse(Long id, Long userId, JobRole role, TeamMemberStatus status) {
    return TeamMemberResponse.builder()
        .id(id)
        .userId(userId)
        .userName("사용자" + userId)
        .role(role)
        .status(status)
        .applicationMessage("지원 메시지")
        .createdAt(LocalDateTime.now())
        .build();
  }
}
