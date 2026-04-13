package com.restartpoint.domain.project.service;

import com.restartpoint.domain.project.dto.*;
import com.restartpoint.domain.project.entity.Checkpoint;
import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.project.entity.ProjectStatus;
import com.restartpoint.domain.project.repository.CheckpointRepository;
import com.restartpoint.domain.project.repository.MemberProgressRepository;
import com.restartpoint.domain.project.repository.ProjectRepository;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.entity.TeamMember;
import com.restartpoint.domain.team.entity.TeamMemberStatus;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.team.repository.TeamRepository;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CheckpointRepository checkpointRepository;

    @Mock
    private MemberProgressRepository memberProgressRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.restartpoint.infra.ai.AiProjectCoachService aiProjectCoachService;

    @InjectMocks
    private ProjectService projectService;

    private User leader;
    private User member;
    private Season season;
    private Team team;
    private Project project;

    @BeforeEach
    void setUp() {
        leader = createUser(1L, "leader@example.com", "팀장");
        member = createUser(2L, "member@example.com", "팀원");
        season = createSeason(1L, "시즌1", SeasonStatus.IN_PROGRESS);
        team = createTeam(1L, "테스트팀", season, leader);
        project = createProject(1L, team, "테스트 프로젝트");
    }

    @Nested
    @DisplayName("프로젝트 생성")
    class CreateProject {

        @Test
        @DisplayName("팀 리더가 프로젝트를 생성할 수 있다")
        void createProjectSuccess() {
            // given
            ProjectCreateRequest request = new ProjectCreateRequest();
            setField(request, "teamId", 1L);
            setField(request, "name", "새 프로젝트");

            given(teamRepository.findById(1L)).willReturn(Optional.of(team));
            given(projectRepository.existsByTeamId(1L)).willReturn(false);
            given(projectRepository.save(any(Project.class))).willAnswer(invocation -> {
                Project saved = invocation.getArgument(0);
                setField(saved, "id", 1L);
                return saved;
            });

            // when
            ProjectResponse response = projectService.createProject(1L, request);

            // then
            assertThat(response.getName()).isEqualTo("새 프로젝트");
            assertThat(response.getStatus()).isEqualTo(ProjectStatus.DRAFT);
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("팀 리더가 아니면 프로젝트를 생성할 수 없다")
        void createProjectFailsWhenNotLeader() {
            // given
            ProjectCreateRequest request = new ProjectCreateRequest();
            setField(request, "teamId", 1L);
            setField(request, "name", "새 프로젝트");

            given(teamRepository.findById(1L)).willReturn(Optional.of(team));

            // when & then
            assertThatThrownBy(() -> projectService.createProject(2L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.NOT_TEAM_LEADER);
                    });
        }

        @Test
        @DisplayName("이미 프로젝트가 있는 팀은 프로젝트를 생성할 수 없다")
        void createProjectFailsWhenAlreadyExists() {
            // given
            ProjectCreateRequest request = new ProjectCreateRequest();
            setField(request, "teamId", 1L);
            setField(request, "name", "새 프로젝트");

            given(teamRepository.findById(1L)).willReturn(Optional.of(team));
            given(projectRepository.existsByTeamId(1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> projectService.createProject(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.PROJECT_ALREADY_EXISTS);
                    });
        }

        @Test
        @DisplayName("시즌이 진행 중이 아니면 프로젝트를 생성할 수 없다")
        void createProjectFailsWhenSeasonNotInProgress() {
            // given
            Season draftSeason = createSeason(2L, "드래프트 시즌", SeasonStatus.DRAFT);
            Team teamInDraftSeason = createTeam(2L, "팀2", draftSeason, leader);

            ProjectCreateRequest request = new ProjectCreateRequest();
            setField(request, "teamId", 2L);
            setField(request, "name", "새 프로젝트");

            given(teamRepository.findById(2L)).willReturn(Optional.of(teamInDraftSeason));
            given(projectRepository.existsByTeamId(2L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> projectService.createProject(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.SEASON_NOT_IN_PROGRESS);
                    });
        }
    }

    @Nested
    @DisplayName("프로젝트 수정")
    class UpdateProject {

        @Test
        @DisplayName("팀원이 프로젝트를 수정할 수 있다")
        void updateProjectByMember() {
            // given
            ProjectUpdateRequest request = new ProjectUpdateRequest();
            setField(request, "name", "수정된 프로젝트");
            setField(request, "problemDefinition", "문제 정의");
            setField(request, "targetUsers", "타깃 사용자");
            setField(request, "solution", "솔루션");

            TeamMember teamMember = TeamMember.builder()
                    .team(team)
                    .user(member)
                    .build();
            teamMember.accept();
            setField(teamMember, "id", 1L);

            given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
            given(teamMemberRepository.findByTeamId(1L)).willReturn(List.of(teamMember));

            // when
            ProjectResponse response = projectService.updateProject(2L, 1L, request);

            // then
            assertThat(response.getName()).isEqualTo("수정된 프로젝트");
        }

        @Test
        @DisplayName("제출 완료된 프로젝트는 수정할 수 없다")
        void updateProjectFailsWhenSubmitted() {
            // given
            project.submit("회고");

            ProjectUpdateRequest request = new ProjectUpdateRequest();
            setField(request, "name", "수정된 프로젝트");

            given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));

            // when & then
            assertThatThrownBy(() -> projectService.updateProject(1L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.PROJECT_ALREADY_SUBMITTED);
                    });
        }
    }

    @Nested
    @DisplayName("프로젝트 시작")
    class StartProject {

        @Test
        @DisplayName("팀 리더가 DRAFT 상태의 프로젝트를 시작할 수 있다")
        void startProjectSuccess() {
            // given
            given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));

            // when
            ProjectResponse response = projectService.startProject(1L, 1L);

            // then
            assertThat(response.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("이미 진행 중인 프로젝트는 시작할 수 없다")
        void startProjectFailsWhenAlreadyInProgress() {
            // given
            project.startProject();
            given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));

            // when & then
            assertThatThrownBy(() -> projectService.startProject(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.INVALID_PROJECT_STATUS);
                    });
        }
    }

    @Nested
    @DisplayName("체크포인트 생성")
    class CreateCheckpoint {

        @Test
        @DisplayName("팀원이 체크포인트를 생성할 수 있다")
        void createCheckpointSuccess() {
            // given
            project.startProject();

            CheckpointCreateRequest request = new CheckpointCreateRequest();
            setField(request, "weekNumber", 1);
            setField(request, "weeklyGoal", "이번 주 목표");
            setField(request, "progressSummary", "진행 상황");

            given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
            given(userRepository.findById(1L)).willReturn(Optional.of(leader));
            given(checkpointRepository.existsByProjectIdAndWeekNumber(1L, 1)).willReturn(false);
            given(checkpointRepository.save(any(Checkpoint.class))).willAnswer(invocation -> {
                Checkpoint saved = invocation.getArgument(0);
                setField(saved, "id", 1L);
                return saved;
            });

            // when
            CheckpointResponse response = projectService.createCheckpoint(1L, 1L, request);

            // then
            assertThat(response.getWeekNumber()).isEqualTo(1);
            assertThat(response.getWeeklyGoal()).isEqualTo("이번 주 목표");
            verify(checkpointRepository).save(any(Checkpoint.class));
        }

        @Test
        @DisplayName("같은 주차에 체크포인트가 이미 존재하면 생성할 수 없다")
        void createCheckpointFailsWhenAlreadyExists() {
            // given
            project.startProject();

            CheckpointCreateRequest request = new CheckpointCreateRequest();
            setField(request, "weekNumber", 1);

            given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
            given(userRepository.findById(1L)).willReturn(Optional.of(leader));
            given(checkpointRepository.existsByProjectIdAndWeekNumber(1L, 1)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> projectService.createCheckpoint(1L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.CHECKPOINT_ALREADY_EXISTS);
                    });
        }
    }

    @Nested
    @DisplayName("프로젝트 제출")
    class SubmitProject {

        @Test
        @DisplayName("팀 리더가 필수 항목이 채워진 프로젝트를 제출할 수 있다")
        void submitProjectSuccess() {
            // given
            project.startProject(); // 프로젝트 시작 (IN_PROGRESS 상태로 변경)
            project.update("프로젝트명", "문제 정의", "타깃 사용자", "솔루션",
                    null, null, null, null, null);

            ProjectSubmitRequest request = new ProjectSubmitRequest();
            setField(request, "teamRetrospective", "팀 회고");

            given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));

            // when
            ProjectResponse response = projectService.submitProject(1L, 1L, request);

            // then
            assertThat(response.getStatus()).isEqualTo(ProjectStatus.SUBMITTED);
            assertThat(response.getTeamRetrospective()).isEqualTo("팀 회고");
        }

        @Test
        @DisplayName("필수 항목이 비어있으면 제출할 수 없다")
        void submitProjectFailsWhenMissingRequired() {
            // given
            project.startProject(); // 프로젝트 시작 (IN_PROGRESS 상태로 변경)

            ProjectSubmitRequest request = new ProjectSubmitRequest();
            setField(request, "teamRetrospective", "팀 회고");

            given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));

            // when & then
            assertThatThrownBy(() -> projectService.submitProject(1L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
                    });
        }

        @Test
        @DisplayName("DRAFT 상태의 프로젝트는 제출할 수 없다")
        void submitProjectFailsWhenDraft() {
            // given (프로젝트는 DRAFT 상태)
            project.update("프로젝트명", "문제 정의", "타깃 사용자", "솔루션",
                    null, null, null, null, null);

            ProjectSubmitRequest request = new ProjectSubmitRequest();
            setField(request, "teamRetrospective", "팀 회고");

            given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));

            // when & then
            assertThatThrownBy(() -> projectService.submitProject(1L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.INVALID_PROJECT_STATUS);
                    });
        }
    }

    @Nested
    @DisplayName("체크포인트 생성 워크플로우")
    class CreateCheckpointWorkflow {

        @Test
        @DisplayName("DRAFT 상태의 프로젝트에서는 체크포인트를 생성할 수 없다")
        void createCheckpointFailsWhenDraft() {
            // given (프로젝트는 DRAFT 상태)
            CheckpointCreateRequest request = new CheckpointCreateRequest();
            setField(request, "weekNumber", 1);

            given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
            given(userRepository.findById(1L)).willReturn(Optional.of(leader));

            // when & then
            assertThatThrownBy(() -> projectService.createCheckpoint(1L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.INVALID_PROJECT_STATUS);
                    });
        }
    }

    @Nested
    @DisplayName("우수작 지정")
    class FeaturedProject {

        @Test
        @DisplayName("운영자는 제출된 프로젝트를 우수작으로 지정할 수 있다")
        void markProjectAsFeaturedSuccess() {
            // given
            setField(season, "status", SeasonStatus.COMPLETED);
            project.submit("회고");

            given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
            given(projectRepository.findFeaturedProjectsBySeasonId(1L)).willReturn(List.of(project));

            // when
            ProjectResponse response = projectService.markProjectAsFeatured(1L);

            // then
            assertThat(response.getFeaturedRank()).isEqualTo(1);
            assertThat(project.getFeaturedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 생성 실패로 남아 있던 우수작 지정은 기존 순번을 유지한다")
        void keepExistingFeaturedRank() {
            // given
            setField(season, "status", SeasonStatus.REVIEWING);
            project.submit("회고");
            project.markAsFeatured(3);

            given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
            given(projectRepository.findFeaturedProjectsBySeasonId(1L)).willReturn(List.of(project));

            // when
            ProjectResponse response = projectService.markProjectAsFeatured(1L);

            // then
            assertThat(response.getFeaturedRank()).isEqualTo(1);
        }

        @Test
        @DisplayName("우수작 해제 후 다시 지정하면 빈 번호부터 다시 채운다")
        void reuseLowestAvailableFeaturedRank() {
            // given
            setField(season, "status", SeasonStatus.COMPLETED);
            project.submit("회고");

            Project first = createProject(2L, team, "우수작 1");
            first.submit("회고");
            first.markAsFeatured(1);

            Project third = createProject(3L, team, "우수작 3");
            third.submit("회고");
            third.markAsFeatured(3);

            given(projectRepository.findByIdWithTeam(1L)).willReturn(Optional.of(project));
            given(projectRepository.findFeaturedProjectsBySeasonId(1L)).willReturn(List.of(first, project, third));

            // when
            ProjectResponse response = projectService.markProjectAsFeatured(1L);

            // then
            assertThat(first.getFeaturedRank()).isEqualTo(1);
            assertThat(response.getFeaturedRank()).isEqualTo(2);
            assertThat(third.getFeaturedRank()).isEqualTo(3);
        }
    }

    // Helper methods
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

    private Season createSeason(Long id, String title, SeasonStatus status) {
        Season season = Season.builder()
                .title(title)
                .description("시즌 설명")
                .recruitmentStartAt(LocalDateTime.now().minusDays(30))
                .recruitmentEndAt(LocalDateTime.now().minusDays(20))
                .teamBuildingStartAt(LocalDateTime.now().minusDays(20))
                .teamBuildingEndAt(LocalDateTime.now().minusDays(10))
                .projectStartAt(LocalDateTime.now().minusDays(10))
                .projectEndAt(LocalDateTime.now().plusDays(20))
                .reviewStartAt(LocalDateTime.now().plusDays(20))
                .reviewEndAt(LocalDateTime.now().plusDays(30))
                .expertReviewWeight(100)
                .candidateReviewWeight(0)
                .build();
        setField(season, "id", id);
        setField(season, "status", status);
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

    private Project createProject(Long id, Team team, String name) {
        Project project = Project.builder()
                .team(team)
                .name(name)
                .build();
        setField(project, "id", id);
        setField(project, "checkpoints", new ArrayList<>());
        return project;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("테스트 필드 설정에 실패했습니다: " + fieldName, exception);
        }
    }
}
