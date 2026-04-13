package com.restartpoint.domain.review.service;

import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.project.entity.ProjectStatus;
import com.restartpoint.domain.project.repository.ProjectRepository;
import com.restartpoint.domain.review.dto.ReviewCreateRequest;
import com.restartpoint.domain.review.dto.ReviewResponse;
import com.restartpoint.domain.review.dto.ReviewScoreRequest;
import com.restartpoint.domain.review.dto.ReviewSummaryResponse;
import com.restartpoint.domain.review.entity.*;
import com.restartpoint.domain.review.repository.ReviewGuideCompletionRepository;
import com.restartpoint.domain.review.repository.ReviewRepository;
import com.restartpoint.domain.review.repository.ReviewScoreRepository;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.user.entity.CertificationStatus;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private ReviewScoreRepository reviewScoreRepository;

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TeamMemberRepository teamMemberRepository;

  @Mock
  private SeasonRepository seasonRepository;

  @Mock
  private ReviewGuideCompletionRepository guideCompletionRepository;

  @InjectMocks
  private ReviewService reviewService;

  @Nested
  @DisplayName("심사 제출")
  class CreateReview {

    @Test
    @DisplayName("수료 인증된 사용자가 심사 기간에 심사를 제출할 수 있다")
    void createReviewSuccess() {
      // given
      User reviewer = createCertifiedUser(1L, "reviewer@example.com", "심사자");
      User projectOwner = createCertifiedUser(2L, "owner@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.REVIEWING);
      Team team = createTeam(1L, "팀이름", season, projectOwner);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      ReviewCreateRequest request = createReviewRequest();

      given(userRepository.findById(1L)).willReturn(Optional.of(reviewer));
      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(guideCompletionRepository.existsByUserIdAndFullyCompletedTrue(1L)).willReturn(true);
      given(teamMemberRepository.findByTeamId(1L)).willReturn(List.of());
      given(reviewRepository.existsByProjectIdAndReviewerId(1L, 1L)).willReturn(false);
      given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> {
        Review review = invocation.getArgument(0);
        setField(review, "id", 1L);
        return review;
      });

      // when
      ReviewResponse response = reviewService.createReview(1L, 1L, request);

      // then
      assertThat(response).isNotNull();
      verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("수료 인증되지 않은 사용자는 심사를 제출할 수 없다")
    void createReviewFailsWhenNotCertified() {
      // given
      User uncertifiedUser = createUser(1L, "user@example.com", "사용자");
      User projectOwner = createCertifiedUser(2L, "owner@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.REVIEWING);
      Team team = createTeam(1L, "팀이름", season, projectOwner);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      ReviewCreateRequest request = createReviewRequest();

      given(userRepository.findById(1L)).willReturn(Optional.of(uncertifiedUser));
      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));

      // when & then
      assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .satisfies(exception -> {
            BusinessException businessException = (BusinessException) exception;
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.NOT_CERTIFIED_REVIEWER);
          });

      verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("심사 기간이 아니면 심사를 제출할 수 없다")
    void createReviewFailsWhenNotReviewingPeriod() {
      // given
      User reviewer = createCertifiedUser(1L, "reviewer@example.com", "심사자");
      User projectOwner = createCertifiedUser(2L, "owner@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.IN_PROGRESS); // 프로젝트 진행 중
      Team team = createTeam(1L, "팀이름", season, projectOwner);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      ReviewCreateRequest request = createReviewRequest();

      given(userRepository.findById(1L)).willReturn(Optional.of(reviewer));
      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(guideCompletionRepository.existsByUserIdAndFullyCompletedTrue(1L)).willReturn(true);

      // when & then
      assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .satisfies(exception -> {
            BusinessException businessException = (BusinessException) exception;
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.SEASON_NOT_REVIEWING);
          });

      verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("제출되지 않은 프로젝트는 심사할 수 없다")
    void createReviewFailsWhenProjectNotSubmitted() {
      // given
      User reviewer = createCertifiedUser(1L, "reviewer@example.com", "심사자");
      User projectOwner = createCertifiedUser(2L, "owner@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.REVIEWING);
      Team team = createTeam(1L, "팀이름", season, projectOwner);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.IN_PROGRESS);
      ReviewCreateRequest request = createReviewRequest();

      given(userRepository.findById(1L)).willReturn(Optional.of(reviewer));
      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(guideCompletionRepository.existsByUserIdAndFullyCompletedTrue(1L)).willReturn(true);

      // when & then
      assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .satisfies(exception -> {
            BusinessException businessException = (BusinessException) exception;
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.PROJECT_NOT_SUBMITTED);
          });

      verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("자신의 프로젝트는 심사할 수 없다")
    void createReviewFailsWhenOwnProject() {
      // given
      User user = createCertifiedUser(1L, "user@example.com", "사용자");
      Season season = createSeason(1L, "시즌1", SeasonStatus.REVIEWING);
      Team team = createTeam(1L, "팀이름", season, user); // 본인이 리더
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      ReviewCreateRequest request = createReviewRequest();

      given(userRepository.findById(1L)).willReturn(Optional.of(user));
      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(guideCompletionRepository.existsByUserIdAndFullyCompletedTrue(1L)).willReturn(true);

      // when & then
      assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .satisfies(exception -> {
            BusinessException businessException = (BusinessException) exception;
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.CANNOT_REVIEW_OWN_PROJECT);
          });

      verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("이미 심사한 프로젝트는 중복 심사할 수 없다")
    void createReviewFailsWhenAlreadyReviewed() {
      // given
      User reviewer = createCertifiedUser(1L, "reviewer@example.com", "심사자");
      User projectOwner = createCertifiedUser(2L, "owner@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.REVIEWING);
      Team team = createTeam(1L, "팀이름", season, projectOwner);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      ReviewCreateRequest request = createReviewRequest();

      given(userRepository.findById(1L)).willReturn(Optional.of(reviewer));
      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(guideCompletionRepository.existsByUserIdAndFullyCompletedTrue(1L)).willReturn(true);
      given(teamMemberRepository.findByTeamId(1L)).willReturn(List.of());
      given(reviewRepository.existsByProjectIdAndReviewerId(1L, 1L)).willReturn(true);

      // when & then
      assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .satisfies(exception -> {
            BusinessException businessException = (BusinessException) exception;
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
          });

      verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("CANDIDATE 심사자는 가이드 학습을 완료해야 심사할 수 있다")
    void createReviewFailsWhenGuideNotCompleted() {
      // given
      User reviewer = createCertifiedUser(1L, "reviewer@example.com", "심사자");
      User projectOwner = createCertifiedUser(2L, "owner@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.REVIEWING);
      Team team = createTeam(1L, "팀이름", season, projectOwner);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      ReviewCreateRequest request = createReviewRequest();

      given(userRepository.findById(1L)).willReturn(Optional.of(reviewer));
      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(guideCompletionRepository.existsByUserIdAndFullyCompletedTrue(1L)).willReturn(false);

      // when & then
      assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .satisfies(exception -> {
            BusinessException businessException = (BusinessException) exception;
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.GUIDE_NOT_COMPLETED);
          });

      verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("REVIEWER 역할 사용자는 EXPERT 타입으로 심사된다")
    void createReviewAsExpert() {
      // given
      User expertReviewer = createExpertReviewer(1L, "expert@example.com", "전문심사자");
      User projectOwner = createCertifiedUser(2L, "owner@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.REVIEWING);
      Team team = createTeam(1L, "팀이름", season, projectOwner);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);
      ReviewCreateRequest request = createReviewRequest();

      given(userRepository.findById(1L)).willReturn(Optional.of(expertReviewer));
      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(teamMemberRepository.findByTeamId(1L)).willReturn(List.of());
      given(reviewRepository.existsByProjectIdAndReviewerId(1L, 1L)).willReturn(false);
      given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> {
        Review review = invocation.getArgument(0);
        setField(review, "id", 1L);
        return review;
      });

      // when
      ReviewResponse response = reviewService.createReview(1L, 1L, request);

      // then
      assertThat(response).isNotNull();
      assertThat(response.getReviewType()).isEqualTo(ReviewType.EXPERT);
    }
  }

  @Nested
  @DisplayName("프로젝트별 심사 조회")
  class GetReviewsByProject {

    @Test
    @DisplayName("팀원이 프로젝트별 심사 목록을 조회할 수 있다")
    void getReviewsByProjectAsTeamMember() {
      // given
      User teamMember = createCertifiedUser(1L, "member@example.com", "팀원");
      Season season = createSeason(1L, "시즌1", SeasonStatus.REVIEWING);
      Team team = createTeam(1L, "팀이름", season, teamMember);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(userRepository.findById(1L)).willReturn(Optional.of(teamMember));
      given(reviewRepository.findByProjectIdWithReviewer(1L)).willReturn(List.of());

      // when
      List<ReviewResponse> responses = reviewService.getReviewsByProject(1L, 1L);

      // then
      assertThat(responses).isEmpty();
      verify(reviewRepository).findByProjectIdWithReviewer(1L);
    }

    @Test
    @DisplayName("운영자는 모든 프로젝트의 심사 목록을 조회할 수 있다")
    void getReviewsByProjectAsAdmin() {
      // given
      User admin = createAdminUser(1L, "admin@example.com", "운영자");
      User projectOwner = createCertifiedUser(2L, "owner@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.REVIEWING);
      Team team = createTeam(1L, "팀이름", season, projectOwner);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(userRepository.findById(1L)).willReturn(Optional.of(admin));
      given(reviewRepository.findByProjectIdWithReviewer(1L)).willReturn(List.of());

      // when
      List<ReviewResponse> responses = reviewService.getReviewsByProject(1L, 1L);

      // then
      assertThat(responses).isEmpty();
      verify(reviewRepository).findByProjectIdWithReviewer(1L);
    }

    @Test
    @DisplayName("권한이 없는 사용자는 심사 목록을 조회할 수 없다")
    void getReviewsByProjectFailsWhenNotAuthorized() {
      // given
      User otherUser = createCertifiedUser(3L, "other@example.com", "다른사람");
      User projectOwner = createCertifiedUser(2L, "owner@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.REVIEWING);
      Team team = createTeam(1L, "팀이름", season, projectOwner);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(userRepository.findById(3L)).willReturn(Optional.of(otherUser));
      given(teamMemberRepository.findByTeamId(1L)).willReturn(List.of());
      given(reviewRepository.existsByProjectIdAndReviewerId(1L, 3L)).willReturn(false);

      // when & then
      assertThatThrownBy(() -> reviewService.getReviewsByProject(3L, 1L))
          .isInstanceOf(BusinessException.class)
          .satisfies(exception -> {
            BusinessException businessException = (BusinessException) exception;
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
          });
    }
  }

  @Nested
  @DisplayName("심사 요약 조회")
  class GetReviewSummary {

    @Test
    @DisplayName("프로젝트 심사 요약을 조회할 수 있다")
    void getReviewSummarySuccess() {
      // given
      User teamMember = createCertifiedUser(1L, "member@example.com", "팀원");
      Season season = createSeason(1L, "시즌1", SeasonStatus.REVIEWING);
      Team team = createTeam(1L, "팀이름", season, teamMember);
      Project project = createProject(1L, "프로젝트", team, ProjectStatus.SUBMITTED);

      given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
      given(userRepository.findById(1L)).willReturn(Optional.of(teamMember));
      given(reviewRepository.findByProjectIdWithReviewer(1L)).willReturn(List.of());

      // when
      ReviewSummaryResponse response = reviewService.getReviewSummary(1L, 1L);

      // then
      assertThat(response).isNotNull();
      assertThat(response.getProjectId()).isEqualTo(1L);
      assertThat(response.getTotalReviewCount()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("내 심사 목록 조회")
  class GetMyReviews {

    @Test
    @DisplayName("내가 심사한 목록을 조회할 수 있다")
    void getMyReviewsSuccess() {
      // given
      given(reviewRepository.findByReviewerIdWithProject(1L)).willReturn(List.of());

      // when
      List<ReviewResponse> responses = reviewService.getMyReviews(1L);

      // then
      assertThat(responses).isEmpty();
      verify(reviewRepository).findByReviewerIdWithProject(1L);
    }
  }

  // 헬퍼 메서드
  private User createUser(Long id, String email, String name) {
    User user = User.builder()
        .email(email)
        .password("encoded-password")
        .name(name)
        .role(Role.USER)
        .build();
    setField(user, "id", id);
    return user;
  }

  private User createCertifiedUser(Long id, String email, String name) {
    User user = User.builder()
        .email(email)
        .password("encoded-password")
        .name(name)
        .role(Role.USER)
        .build();
    setField(user, "id", id);
    user.requestCertification("부트캠프", "1기", "2026-03-01", "https://example.com/cert");
    user.approveCertification();
    return user;
  }

  private User createExpertReviewer(Long id, String email, String name) {
    User user = User.builder()
        .email(email)
        .password("encoded-password")
        .name(name)
        .role(Role.REVIEWER)
        .build();
    setField(user, "id", id);
    return user;
  }

  private User createAdminUser(Long id, String email, String name) {
    User user = User.builder()
        .email(email)
        .password("encoded-password")
        .name(name)
        .role(Role.ADMIN)
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
        .reviewEndAt(now.plusDays(5))
        .expertReviewWeight(70)
        .candidateReviewWeight(30)
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

  private ReviewCreateRequest createReviewRequest() {
    ReviewCreateRequest request = new ReviewCreateRequest();
    List<ReviewScoreRequest> scores = List.of(
        createScoreRequest(RubricItem.PROBLEM_DEFINITION, 5, "잘함"),
        createScoreRequest(RubricItem.USER_VALUE, 4, "좋음"),
        createScoreRequest(RubricItem.AI_USAGE, 5, "훌륭함"),
        createScoreRequest(RubricItem.UX_COMPLETENESS, 4, "괜찮음"),
        createScoreRequest(RubricItem.TECHNICAL_FEASIBILITY, 5, "완벽"),
        createScoreRequest(RubricItem.COLLABORATION, 4, "원활함")
    );
    setField(request, "scores", scores);
    setField(request, "overallComment", "전반적으로 좋은 프로젝트입니다.");
    return request;
  }

  private ReviewScoreRequest createScoreRequest(RubricItem item, int score, String comment) {
    ReviewScoreRequest request = new ReviewScoreRequest();
    setField(request, "rubricItem", item);
    setField(request, "score", score);
    setField(request, "comment", comment);
    return request;
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
