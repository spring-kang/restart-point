package com.restartpoint.domain.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.profile.repository.ProfileRepository;
import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.project.entity.ProjectStatus;
import com.restartpoint.domain.project.repository.ProjectRepository;
import com.restartpoint.domain.report.dto.GrowthReportResponse;
import com.restartpoint.domain.report.entity.GrowthReport;
import com.restartpoint.domain.report.entity.ReportType;
import com.restartpoint.domain.report.repository.GrowthReportRepository;
import com.restartpoint.domain.review.dto.ReviewSummaryResponse;
import com.restartpoint.domain.review.entity.RubricItem;
import com.restartpoint.domain.review.service.ReviewService;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.infra.ai.AiGrowthReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrowthReportServiceTest {

  @Mock
  private GrowthReportRepository growthReportRepository;

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TeamMemberRepository teamMemberRepository;

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private ReviewService reviewService;

  @Mock
  private AiGrowthReportService aiGrowthReportService;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private GrowthReportService growthReportService;

  @Nested
  @DisplayName("팀 리포트 조회")
  class GetTeamReport {

    @Test
    @DisplayName("팀원이 팀 리포트를 조회할 수 있다")
    void getTeamReportSuccess() {
      // given
      User leader = createUser(1L, "leader@example.com", "팀장", Role.USER);
      Season season = createSeason(1L, "시즌1", SeasonStatus.COMPLETED);
      Team team = createTeam(1L, "팀이름", season, leader);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      GrowthReport report = createTeamReport(1L, project);

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(userRepository.findById(1L)).willReturn(Optional.of(leader));
      given(growthReportRepository.findTeamReportByProjectId(1L)).willReturn(Optional.of(report));

      // when
      GrowthReportResponse response = growthReportService.getTeamReport(1L, 1L);

      // then
      assertThat(response).isNotNull();
      assertThat(response.getReportType()).isEqualTo(ReportType.TEAM);
    }

    @Test
    @DisplayName("팀원이 아닌 사용자는 팀 리포트를 조회할 수 없다")
    void getTeamReportFailsWhenNotTeamMember() {
      // given
      User leader = createUser(1L, "leader@example.com", "팀장", Role.USER);
      User otherUser = createUser(2L, "other@example.com", "다른사람", Role.USER);
      Season season = createSeason(1L, "시즌1", SeasonStatus.COMPLETED);
      Team team = createTeam(1L, "팀이름", season, leader);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(userRepository.findById(2L)).willReturn(Optional.of(otherUser));
      given(teamMemberRepository.findByTeamId(1L)).willReturn(List.of());

      // when & then
      assertThatThrownBy(() -> growthReportService.getTeamReport(2L, 1L))
          .isInstanceOf(BusinessException.class)
          .satisfies(exception -> {
            BusinessException businessException = (BusinessException) exception;
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
          });
    }

    @Test
    @DisplayName("운영자는 모든 팀 리포트를 조회할 수 있다")
    void getTeamReportAsAdmin() {
      // given
      User admin = createUser(2L, "admin@example.com", "운영자", Role.ADMIN);
      User leader = createUser(1L, "leader@example.com", "팀장", Role.USER);
      Season season = createSeason(1L, "시즌1", SeasonStatus.COMPLETED);
      Team team = createTeam(1L, "팀이름", season, leader);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      GrowthReport report = createTeamReport(1L, project);

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(userRepository.findById(2L)).willReturn(Optional.of(admin));
      given(growthReportRepository.findTeamReportByProjectId(1L)).willReturn(Optional.of(report));

      // when
      GrowthReportResponse response = growthReportService.getTeamReport(2L, 1L);

      // then
      assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("리포트가 없으면 예외가 발생한다")
    void getTeamReportFailsWhenNotFound() {
      // given
      User leader = createUser(1L, "leader@example.com", "팀장", Role.USER);
      Season season = createSeason(1L, "시즌1", SeasonStatus.COMPLETED);
      Team team = createTeam(1L, "팀이름", season, leader);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(userRepository.findById(1L)).willReturn(Optional.of(leader));
      given(growthReportRepository.findTeamReportByProjectId(1L)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> growthReportService.getTeamReport(1L, 1L))
          .isInstanceOf(BusinessException.class)
          .satisfies(exception -> {
            BusinessException businessException = (BusinessException) exception;
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.REPORT_NOT_FOUND);
          });
    }
  }

  @Nested
  @DisplayName("개인 리포트 조회")
  class GetIndividualReport {

    @Test
    @DisplayName("본인의 개인 리포트를 조회할 수 있다")
    void getIndividualReportSuccess() {
      // given
      User user = createUser(1L, "user@example.com", "사용자", Role.USER);
      Season season = createSeason(1L, "시즌1", SeasonStatus.COMPLETED);
      Team team = createTeam(1L, "팀이름", season, user);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      GrowthReport report = createIndividualReport(1L, project, user);

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(userRepository.findById(1L)).willReturn(Optional.of(user));
      given(growthReportRepository.findIndividualReport(1L, 1L)).willReturn(Optional.of(report));

      // when
      GrowthReportResponse response = growthReportService.getIndividualReport(1L, 1L);

      // then
      assertThat(response).isNotNull();
      assertThat(response.getReportType()).isEqualTo(ReportType.INDIVIDUAL);
    }
  }

  @Nested
  @DisplayName("내 리포트 목록 조회")
  class GetMyReports {

    @Test
    @DisplayName("사용자의 모든 리포트 목록을 조회할 수 있다")
    void getMyReportsSuccess() {
      // given
      User user = createUser(1L, "user@example.com", "사용자", Role.USER);
      Season season = createSeason(1L, "시즌1", SeasonStatus.COMPLETED);
      Team team = createTeam(1L, "팀이름", season, user);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      GrowthReport report = createIndividualReport(1L, project, user);

      given(growthReportRepository.findAllByUserId(1L)).willReturn(List.of(report));

      // when
      List<GrowthReportResponse> responses = growthReportService.getMyReports(1L);

      // then
      assertThat(responses).hasSize(1);
    }
  }

  @Nested
  @DisplayName("리포트 생성")
  class GenerateReports {

    @Test
    @DisplayName("이미 리포트가 생성된 프로젝트는 재생성하지 않는다")
    void skipAlreadyGeneratedReport() {
      // given
      User leader = createUser(1L, "leader@example.com", "팀장", Role.USER);
      Season season = createSeason(1L, "시즌1", SeasonStatus.COMPLETED);
      Team team = createTeam(1L, "팀이름", season, leader);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      GrowthReport generatedTeamReport = createTeamReport(10L, project);
      generatedTeamReport.updateContent("강점", "보완점", null, "다음 액션", null, "추천 영역");
      GrowthReport generatedIndividualReport = createIndividualReport(11L, project, leader);
      generatedIndividualReport.updateContent(null, null, "역할 피드백", "다음 액션", "포트폴리오", "추천 영역");
      ReviewSummaryResponse reviewSummary = createReviewSummary(project.getId(), project.getName());

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(reviewService.getReviewSummaryInternal(1L)).willReturn(reviewSummary);
      given(growthReportRepository.findAllByProjectId(1L))
          .willReturn(List.of(generatedTeamReport, generatedIndividualReport));

      // when
      growthReportService.generateReportsForProject(1L);

      // then
      verify(growthReportRepository, never()).save(any(GrowthReport.class));
      verify(aiGrowthReportService, never()).generateTeamReport(any(), any());
    }

    @Test
    @DisplayName("제출되지 않은 프로젝트는 리포트를 생성하지 않는다")
    void skipNotSubmittedProject() {
      // given
      User leader = createUser(1L, "leader@example.com", "팀장", Role.USER);
      Season season = createSeason(1L, "시즌1", SeasonStatus.COMPLETED);
      Team team = createTeam(1L, "팀이름", season, leader);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.IN_PROGRESS);

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));

      // when
      growthReportService.generateReportsForProject(1L);

      // then
      verify(growthReportRepository, never()).save(any(GrowthReport.class));
    }

    @Test
    @DisplayName("AI 생성이 실패해도 fallback 내용으로 팀과 개인 리포트를 생성한다")
    void generateReportsWithFallbackWhenAiFails() throws Exception {
      // given
      User leader = createUser(1L, "leader@example.com", "팀장", Role.USER);
      Season season = createSeason(1L, "시즌1", SeasonStatus.COMPLETED);
      Team team = createTeam(1L, "팀이름", season, leader);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      ReviewSummaryResponse reviewSummary = createReviewSummary(project.getId(), project.getName());

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(growthReportRepository.findAllByProjectId(1L)).willReturn(List.of());
      given(reviewService.getReviewSummaryInternal(1L)).willReturn(reviewSummary);
      given(objectMapper.writeValueAsString(any())).willReturn("{\"summary\":true}");
      given(aiGrowthReportService.generateTeamReport(project, reviewSummary)).willReturn(null);
      given(aiGrowthReportService.generateIndividualReport(eq(project), eq(reviewSummary), eq(1L), eq("팀장"), any()))
          .willReturn(null);
      given(teamMemberRepository.findByTeamId(1L)).willReturn(List.of());

      // when
      growthReportService.generateReportsForProject(1L);

      // then
      verify(growthReportRepository, times(2)).save(any(GrowthReport.class));
      verify(aiGrowthReportService).generateTeamReport(project, reviewSummary);
      verify(aiGrowthReportService).generateIndividualReport(eq(project), eq(reviewSummary), eq(1L), eq("팀장"), any());
    }

    @Test
    @DisplayName("생성 실패로 남은 리포트는 기존 row를 재사용해 fallback으로 채운다")
    void regenerateExistingUngeneratedReports() throws Exception {
      // given
      User leader = createUser(1L, "leader@example.com", "팀장", Role.USER);
      Season season = createSeason(1L, "시즌1", SeasonStatus.COMPLETED);
      Team team = createTeam(1L, "팀이름", season, leader);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.COMPLETED);
      ReviewSummaryResponse reviewSummary = createReviewSummary(project.getId(), project.getName());
      GrowthReport pendingTeamReport = createTeamReport(10L, project);
      GrowthReport pendingIndividualReport = createIndividualReport(11L, project, leader);

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(growthReportRepository.findAllByProjectId(1L))
          .willReturn(List.of(pendingTeamReport, pendingIndividualReport));
      given(reviewService.getReviewSummaryInternal(1L)).willReturn(reviewSummary);
      given(aiGrowthReportService.generateTeamReport(project, reviewSummary)).willReturn(null);
      given(aiGrowthReportService.generateIndividualReport(eq(project), eq(reviewSummary), eq(1L), eq("팀장"), any()))
          .willReturn(null);
      given(teamMemberRepository.findByTeamId(1L)).willReturn(List.of());

      // when
      growthReportService.generateReportsForProject(1L);

      // then
      assertThat(pendingTeamReport.isGenerated()).isTrue();
      assertThat(pendingTeamReport.getTeamStrengths()).isNotBlank();
      assertThat(pendingIndividualReport.isGenerated()).isTrue();
      assertThat(pendingIndividualReport.getRoleSpecificFeedback()).isNotBlank();
      verify(growthReportRepository, never()).save(any(GrowthReport.class));
    }
  }

  @Nested
  @DisplayName("프로젝트 리포트 조회")
  class GetProjectReports {

    @Test
    @DisplayName("프로젝트의 팀 리포트와 본인 개인 리포트를 조회할 수 있다")
    void getProjectReportsSuccess() {
      // given
      User user = createUser(1L, "user@example.com", "사용자", Role.USER);
      Season season = createSeason(1L, "시즌1", SeasonStatus.COMPLETED);
      Team team = createTeam(1L, "팀이름", season, user);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      GrowthReport teamReport = createTeamReport(1L, project);
      GrowthReport individualReport = createIndividualReport(2L, project, user);

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(userRepository.findById(1L)).willReturn(Optional.of(user));
      given(growthReportRepository.findTeamReportByProjectId(1L)).willReturn(Optional.of(teamReport));
      given(growthReportRepository.findIndividualReport(1L, 1L)).willReturn(Optional.of(individualReport));

      // when
      List<GrowthReportResponse> responses = growthReportService.getProjectReports(1L, 1L);

      // then
      assertThat(responses).hasSize(2);
    }
  }

  // 헬퍼 메서드
  private User createUser(Long id, String email, String name, Role role) {
    User user = User.builder()
        .email(email)
        .password("encoded-password")
        .name(name)
        .role(role)
        .build();
    setField(user, "id", id);
    return user;
  }

  private Season createSeason(Long id, String title, SeasonStatus status) {
    LocalDateTime now = LocalDateTime.now();
    Season season = Season.builder()
        .title(title)
        .description("시즌 설명")
        .recruitmentStartAt(now.minusDays(60))
        .recruitmentEndAt(now.minusDays(50))
        .teamBuildingStartAt(now.minusDays(40))
        .teamBuildingEndAt(now.minusDays(30))
        .projectStartAt(now.minusDays(20))
        .projectEndAt(now.minusDays(10))
        .reviewStartAt(now.minusDays(5))
        .reviewEndAt(now)
        .build();
    setField(season, "id", id);
    season.updateStatus(status);
    return season;
  }

  private Team createTeam(Long id, String name, Season season, User leader) {
    Team team = Team.builder()
        .name(name)
        .description("팀 설명")
        .season(season)
        .leader(leader)
        .build();
    setField(team, "id", id);
    return team;
  }

  private Project createProject(Long id, String name, Team team, ProjectStatus status) {
    Project project = Project.builder()
        .team(team)
        .name(name)
        .problemDefinition("프로젝트 설명")
        .build();
    setField(project, "id", id);
    setField(project, "status", status);
    return project;
  }

  private ReviewSummaryResponse createReviewSummary(Long projectId, String projectName) {
    Map<RubricItem, Double> rubricAverages = new EnumMap<>(RubricItem.class);
    rubricAverages.put(RubricItem.PROBLEM_DEFINITION, 4.5);
    rubricAverages.put(RubricItem.USER_VALUE, 4.2);
    rubricAverages.put(RubricItem.AI_USAGE, 3.8);
    rubricAverages.put(RubricItem.UX_COMPLETENESS, 3.6);
    rubricAverages.put(RubricItem.TECHNICAL_FEASIBILITY, 3.2);
    rubricAverages.put(RubricItem.COLLABORATION, 4.0);

    return ReviewSummaryResponse.builder()
        .projectId(projectId)
        .projectName(projectName)
        .totalReviewCount(1)
        .expertReviewCount(1)
        .candidateReviewCount(0)
        .weightedAverageScore(3.9)
        .expertAverageScore(3.9)
        .candidateAverageScore(0.0)
        .rubricAverages(rubricAverages)
        .expertRubricAverages(rubricAverages)
        .candidateRubricAverages(Map.of())
        .build();
  }

  private GrowthReport createTeamReport(Long id, Project project) {
    GrowthReport report = GrowthReport.builder()
        .project(project)
        .reportType(ReportType.TEAM)
        .build();
    setField(report, "id", id);
    return report;
  }

  private GrowthReport createIndividualReport(Long id, Project project, User user) {
    GrowthReport report = GrowthReport.builder()
        .project(project)
        .user(user)
        .reportType(ReportType.INDIVIDUAL)
        .build();
    setField(report, "id", id);
    return report;
  }

  private void setField(Object target, String fieldName, Object value) {
    try {
      Field field = findField(target.getClass(), fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("테스트 필드 설정에 실패했습니다: " + fieldName, exception);
    }
  }

  private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }
}
