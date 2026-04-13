package com.restartpoint.domain.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.project.dto.*;
import com.restartpoint.domain.project.entity.ProjectStatus;
import com.restartpoint.domain.project.service.ProjectService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ProjectService projectService;

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
  @DisplayName("프로젝트 생성 API")
  class CreateProjectApi {

    @Test
    @DisplayName("프로젝트 생성에 성공한다")
    void createProjectSuccess() throws Exception {
      // given
      setAuthentication(1L, "leader@test.com", "USER");
      ProjectCreateRequest request = createProjectCreateRequest();
      ProjectResponse response = createProjectResponse(1L, "테스트 프로젝트", ProjectStatus.DRAFT);

      given(projectService.createProject(eq(1L), any(ProjectCreateRequest.class))).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/v1/projects")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.id").value(1))
          .andExpect(jsonPath("$.data.name").value("테스트 프로젝트"))
          .andExpect(jsonPath("$.message").value("프로젝트가 생성되었습니다."));
    }

    @Test
    @DisplayName("팀 리더가 아니면 프로젝트 생성에 실패한다")
    void createProjectFailsWhenNotLeader() throws Exception {
      // given
      setAuthentication(2L, "member@test.com", "USER");
      ProjectCreateRequest request = createProjectCreateRequest();

      given(projectService.createProject(eq(2L), any(ProjectCreateRequest.class)))
          .willThrow(new BusinessException(ErrorCode.NOT_TEAM_LEADER));

      // when & then
      mockMvc.perform(post("/api/v1/projects")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.errorCode").value("TEAM_004"));
    }
  }

  @Nested
  @DisplayName("프로젝트 조회 API")
  class GetProjectApi {

    @Test
    @DisplayName("프로젝트 상세 조회에 성공한다")
    void getProjectSuccess() throws Exception {
      // given
      setAuthentication(1L, "member@test.com", "USER");
      ProjectResponse response = createProjectResponse(1L, "테스트 프로젝트", ProjectStatus.IN_PROGRESS);

      given(projectService.getProject(1L, 1L)).willReturn(response);

      // when & then
      mockMvc.perform(get("/api/v1/projects/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.id").value(1))
          .andExpect(jsonPath("$.data.name").value("테스트 프로젝트"));
    }

    @Test
    @DisplayName("팀원이 아니면 프로젝트 조회에 실패한다")
    void getProjectFailsWhenNotTeamMember() throws Exception {
      // given
      setAuthentication(3L, "other@test.com", "USER");

      given(projectService.getProject(3L, 1L))
          .willThrow(new BusinessException(ErrorCode.ACCESS_DENIED));

      // when & then
      mockMvc.perform(get("/api/v1/projects/1"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("팀의 프로젝트 조회에 성공한다")
    void getProjectByTeamSuccess() throws Exception {
      // given
      setAuthentication(1L, "member@test.com", "USER");
      ProjectResponse response = createProjectResponse(1L, "테스트 프로젝트", ProjectStatus.IN_PROGRESS);

      given(projectService.getProjectByTeam(1L, 1L)).willReturn(response);

      // when & then
      mockMvc.perform(get("/api/v1/teams/1/project"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.name").value("테스트 프로젝트"));
    }

    @Test
    @DisplayName("시즌별 프로젝트 목록 조회에 성공한다")
    void getProjectsBySeasonSuccess() throws Exception {
      // given
      var projects = new PageImpl<>(List.of(
          createProjectResponse(1L, "프로젝트1", ProjectStatus.IN_PROGRESS),
          createProjectResponse(2L, "프로젝트2", ProjectStatus.SUBMITTED)
      ));
      given(projectService.getProjectsBySeason(eq(1L), any(Pageable.class))).willReturn(projects);

      // when & then
      mockMvc.perform(get("/api/v1/seasons/1/projects"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("우수작 목록 조회에 성공한다")
    void getFeaturedProjectsSuccess() throws Exception {
      // given
      List<ProjectResponse> responses = List.of(
          createProjectResponse(1L, "우수작 프로젝트", ProjectStatus.COMPLETED)
      );
      given(projectService.getFeaturedProjects()).willReturn(responses);

      // when & then
      mockMvc.perform(get("/api/v1/projects/featured"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(1))
          .andExpect(jsonPath("$.data[0].name").value("우수작 프로젝트"));
    }
  }

  @Nested
  @DisplayName("프로젝트 수정 API")
  class UpdateProjectApi {

    @Test
    @DisplayName("프로젝트 수정에 성공한다")
    void updateProjectSuccess() throws Exception {
      // given
      setAuthentication(1L, "leader@test.com", "USER");
      ProjectUpdateRequest request = createProjectUpdateRequest();
      ProjectResponse response = createProjectResponse(1L, "수정된 프로젝트", ProjectStatus.IN_PROGRESS);

      given(projectService.updateProject(eq(1L), eq(1L), any(ProjectUpdateRequest.class))).willReturn(response);

      // when & then
      mockMvc.perform(put("/api/v1/projects/1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.name").value("수정된 프로젝트"))
          .andExpect(jsonPath("$.message").value("프로젝트가 수정되었습니다."));
    }
  }

  @Nested
  @DisplayName("프로젝트 상태 변경 API")
  class ProjectStatusApi {

    @Test
    @DisplayName("프로젝트 시작에 성공한다")
    void startProjectSuccess() throws Exception {
      // given
      setAuthentication(1L, "leader@test.com", "USER");
      ProjectResponse response = createProjectResponse(1L, "프로젝트", ProjectStatus.IN_PROGRESS);

      given(projectService.startProject(1L, 1L)).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/v1/projects/1/start"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
          .andExpect(jsonPath("$.message").value("프로젝트가 시작되었습니다."));
    }

    @Test
    @DisplayName("프로젝트 제출에 성공한다")
    void submitProjectSuccess() throws Exception {
      // given
      setAuthentication(1L, "leader@test.com", "USER");
      ProjectSubmitRequest request = createProjectSubmitRequest();
      ProjectResponse response = createProjectResponse(1L, "프로젝트", ProjectStatus.SUBMITTED);

      given(projectService.submitProject(eq(1L), eq(1L), any(ProjectSubmitRequest.class))).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/v1/projects/1/submit")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
          .andExpect(jsonPath("$.message").value("프로젝트가 제출되었습니다."));
    }
  }

  @Nested
  @DisplayName("체크포인트 API")
  class CheckpointApi {

    @Test
    @DisplayName("체크포인트 생성에 성공한다")
    void createCheckpointSuccess() throws Exception {
      // given
      setAuthentication(1L, "member@test.com", "USER");
      CheckpointCreateRequest request = createCheckpointCreateRequest();
      CheckpointResponse response = createCheckpointResponse(1L, "1주차 진행상황");

      given(projectService.createCheckpoint(eq(1L), eq(1L), any(CheckpointCreateRequest.class)))
          .willReturn(response);

      // when & then
      mockMvc.perform(post("/api/v1/projects/1/checkpoints")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.weeklyGoal").value("1주차 진행상황"))
          .andExpect(jsonPath("$.message").value("체크포인트가 생성되었습니다."));
    }

    @Test
    @DisplayName("체크포인트 목록 조회에 성공한다")
    void getCheckpointsSuccess() throws Exception {
      // given
      setAuthentication(1L, "member@test.com", "USER");
      List<CheckpointResponse> checkpoints = List.of(
          createCheckpointResponse(1L, "1주차"),
          createCheckpointResponse(2L, "2주차")
      );

      given(projectService.getCheckpointsByProject(1L, 1L)).willReturn(checkpoints);

      // when & then
      mockMvc.perform(get("/api/v1/projects/1/checkpoints"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("체크포인트 상세 조회에 성공한다")
    void getCheckpointSuccess() throws Exception {
      // given
      setAuthentication(1L, "member@test.com", "USER");
      CheckpointResponse response = createCheckpointResponse(1L, "1주차 진행상황");

      given(projectService.getCheckpoint(1L, 1L)).willReturn(response);

      // when & then
      mockMvc.perform(get("/api/v1/checkpoints/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("체크포인트 수정에 성공한다")
    void updateCheckpointSuccess() throws Exception {
      // given
      setAuthentication(1L, "member@test.com", "USER");
      CheckpointUpdateRequest request = createCheckpointUpdateRequest();
      CheckpointResponse response = createCheckpointResponse(1L, "수정된 체크포인트");

      given(projectService.updateCheckpoint(eq(1L), eq(1L), any(CheckpointUpdateRequest.class)))
          .willReturn(response);

      // when & then
      mockMvc.perform(put("/api/v1/checkpoints/1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("체크포인트가 수정되었습니다."));
    }

    @Test
    @DisplayName("체크포인트 삭제에 성공한다")
    void deleteCheckpointSuccess() throws Exception {
      // given
      setAuthentication(1L, "member@test.com", "USER");
      doNothing().when(projectService).deleteCheckpoint(1L, 1L);

      // when & then
      mockMvc.perform(delete("/api/v1/checkpoints/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("체크포인트가 삭제되었습니다."));

      verify(projectService).deleteCheckpoint(1L, 1L);
    }

    @Test
    @DisplayName("AI 피드백 재생성에 성공한다")
    void regenerateAiFeedbackSuccess() throws Exception {
      // given
      setAuthentication(1L, "member@test.com", "USER");
      CheckpointResponse response = createCheckpointResponse(1L, "체크포인트");

      given(projectService.regenerateAiFeedback(1L, 1L)).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/v1/checkpoints/1/ai-feedback"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("AI 피드백이 재생성되었습니다."));
    }
  }

  @Nested
  @DisplayName("우수작 지정 API")
  class FeaturedProjectApi {

    @Test
    @DisplayName("우수작 지정에 성공한다")
    void markFeaturedProjectSuccess() throws Exception {
      // given
      setAuthentication(4L, "admin@test.com", "ADMIN");
      ProjectResponse response = createProjectResponse(1L, "우수작 프로젝트", ProjectStatus.COMPLETED);

      given(projectService.markProjectAsFeatured(1L)).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/v1/admin/projects/1/featured"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("우수작으로 지정되었습니다."));
    }

    @Test
    @DisplayName("우수작 지정 해제에 성공한다")
    void unmarkFeaturedProjectSuccess() throws Exception {
      // given
      setAuthentication(4L, "admin@test.com", "ADMIN");
      ProjectResponse response = createProjectResponse(1L, "우수작 프로젝트", ProjectStatus.COMPLETED);

      given(projectService.unmarkProjectAsFeatured(1L)).willReturn(response);

      // when & then
      mockMvc.perform(delete("/api/v1/admin/projects/1/featured"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("우수작 지정이 해제되었습니다."));
    }
  }

  // 헬퍼 메서드
  private ProjectCreateRequest createProjectCreateRequest() {
    ProjectCreateRequest request = new ProjectCreateRequest();
    try {
      var field = ProjectCreateRequest.class.getDeclaredField("teamId");
      field.setAccessible(true);
      field.set(request, 1L);

      field = ProjectCreateRequest.class.getDeclaredField("name");
      field.setAccessible(true);
      field.set(request, "테스트 프로젝트");

      field = ProjectCreateRequest.class.getDeclaredField("problemDefinition");
      field.setAccessible(true);
      field.set(request, "프로젝트 설명");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return request;
  }

  private ProjectUpdateRequest createProjectUpdateRequest() {
    ProjectUpdateRequest request = new ProjectUpdateRequest();
    try {
      var field = ProjectUpdateRequest.class.getDeclaredField("name");
      field.setAccessible(true);
      field.set(request, "수정된 프로젝트");

      field = ProjectUpdateRequest.class.getDeclaredField("problemDefinition");
      field.setAccessible(true);
      field.set(request, "수정된 설명");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return request;
  }

  private ProjectSubmitRequest createProjectSubmitRequest() {
    ProjectSubmitRequest request = new ProjectSubmitRequest();
    try {
      var field = ProjectSubmitRequest.class.getDeclaredField("teamRetrospective");
      field.setAccessible(true);
      field.set(request, "팀 회고 내용입니다.");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return request;
  }

  private CheckpointCreateRequest createCheckpointCreateRequest() {
    CheckpointCreateRequest request = new CheckpointCreateRequest();
    try {
      var field = CheckpointCreateRequest.class.getDeclaredField("weekNumber");
      field.setAccessible(true);
      field.set(request, 1);

      field = CheckpointCreateRequest.class.getDeclaredField("weeklyGoal");
      field.setAccessible(true);
      field.set(request, "1주차 진행상황");

      field = CheckpointCreateRequest.class.getDeclaredField("progressSummary");
      field.setAccessible(true);
      field.set(request, "이번 주 진행 내용");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return request;
  }

  private CheckpointUpdateRequest createCheckpointUpdateRequest() {
    CheckpointUpdateRequest request = new CheckpointUpdateRequest();
    try {
      var field = CheckpointUpdateRequest.class.getDeclaredField("weeklyGoal");
      field.setAccessible(true);
      field.set(request, "수정된 체크포인트");

      field = CheckpointUpdateRequest.class.getDeclaredField("progressSummary");
      field.setAccessible(true);
      field.set(request, "수정된 내용");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return request;
  }

  private ProjectResponse createProjectResponse(Long id, String name, ProjectStatus status) {
    return ProjectResponse.builder()
        .id(id)
        .teamId(1L)
        .teamName("테스트 팀")
        .seasonId(1L)
        .seasonTitle("테스트 시즌")
        .name(name)
        .problemDefinition("프로젝트 설명")
        .status(status)
        .featuredRank(1)
        .createdAt(LocalDateTime.now())
        .build();
  }

  private CheckpointResponse createCheckpointResponse(Long id, String weeklyGoal) {
    return CheckpointResponse.builder()
        .id(id)
        .projectId(1L)
        .weekNumber(1)
        .weeklyGoal(weeklyGoal)
        .progressSummary("체크포인트 내용")
        .createdAt(LocalDateTime.now())
        .build();
  }
}
