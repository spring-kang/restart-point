package com.restartpoint.domain.project.controller;

import com.restartpoint.domain.project.entity.Checkpoint;
import com.restartpoint.domain.project.entity.Project;
import com.restartpoint.domain.project.repository.CheckpointRepository;
import com.restartpoint.domain.project.repository.ProjectRepository;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.repository.TeamRepository;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CheckpointRepository checkpointRepository;

    private User teamLeader;
    private User nonMember;
    private Team team;
    private Project project;
    private Checkpoint checkpoint;

    @BeforeEach
    void setUp() {
        checkpointRepository.deleteAll();
        projectRepository.deleteAll();
        teamRepository.deleteAll();
        seasonRepository.deleteAll();
        userRepository.deleteAll();

        // 팀 리더 생성
        teamLeader = userRepository.save(User.builder()
                .email("leader@example.com")
                .password("password")
                .name("팀리더")
                .role(Role.USER)
                .build());

        // 비팀원 생성
        nonMember = userRepository.save(User.builder()
                .email("nonmember@example.com")
                .password("password")
                .name("비팀원")
                .role(Role.USER)
                .build());

        // 시즌 생성
        Season season = Season.builder()
                .title("테스트 시즌")
                .description("테스트용 시즌입니다")
                .recruitmentStartAt(LocalDateTime.now().minusDays(30))
                .recruitmentEndAt(LocalDateTime.now().minusDays(20))
                .teamBuildingStartAt(LocalDateTime.now().minusDays(19))
                .teamBuildingEndAt(LocalDateTime.now().minusDays(10))
                .projectStartAt(LocalDateTime.now().minusDays(9))
                .projectEndAt(LocalDateTime.now().plusDays(30))
                .reviewStartAt(LocalDateTime.now().plusDays(31))
                .reviewEndAt(LocalDateTime.now().plusDays(40))
                .expertReviewWeight(70)
                .candidateReviewWeight(30)
                .build();
        season.updateStatus(SeasonStatus.IN_PROGRESS);
        season = seasonRepository.save(season);

        // 팀 생성
        team = teamRepository.save(Team.builder()
                .name("테스트팀")
                .description("테스트용 팀입니다")
                .season(season)
                .leader(teamLeader)
                .build());

        // 프로젝트 생성
        project = projectRepository.save(Project.builder()
                .team(team)
                .name("테스트 프로젝트")
                .problemDefinition("문제 정의")
                .targetUsers("타겟 사용자")
                .solution("솔루션")
                .build());
        project.startProject(); // IN_PROGRESS 상태로 변경
        project = projectRepository.save(project);

        // 체크포인트 생성
        checkpoint = checkpointRepository.save(Checkpoint.builder()
                .project(project)
                .weekNumber(1)
                .weeklyGoal("1주차 목표")
                .progressSummary("진행 상황")
                .createdBy(teamLeader)
                .build());
    }

    @Nested
    @DisplayName("비인증 사용자 접근 테스트")
    class UnauthenticatedAccessTest {

        @Test
        @DisplayName("비인증 사용자는 프로젝트 상세 조회가 불가능하다")
        void unauthenticatedUserCannotAccessProject() throws Exception {
            mockMvc.perform(get("/api/v1/projects/" + project.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("비인증 사용자는 팀 프로젝트 조회가 불가능하다")
        void unauthenticatedUserCannotAccessTeamProject() throws Exception {
            mockMvc.perform(get("/api/v1/teams/" + team.getId() + "/project"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("비인증 사용자는 프로젝트 체크포인트 목록 조회가 불가능하다")
        void unauthenticatedUserCannotAccessProjectCheckpoints() throws Exception {
            mockMvc.perform(get("/api/v1/projects/" + project.getId() + "/checkpoints"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("비인증 사용자는 체크포인트 상세 조회가 불가능하다")
        void unauthenticatedUserCannotAccessCheckpoint() throws Exception {
            mockMvc.perform(get("/api/v1/checkpoints/" + checkpoint.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("비팀원 사용자 접근 테스트")
    class NonTeamMemberAccessTest {

        private String nonMemberToken;

        @BeforeEach
        void setUpToken() {
            nonMemberToken = jwtTokenProvider.createToken(
                    nonMember.getId(), nonMember.getEmail(), nonMember.getRole().name());
        }

        @Test
        @DisplayName("비팀원은 프로젝트 상세 조회가 불가능하다")
        void nonTeamMemberCannotAccessProject() throws Exception {
            mockMvc.perform(get("/api/v1/projects/" + project.getId())
                            .header("Authorization", "Bearer " + nonMemberToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("PROJECT_007"));
        }

        @Test
        @DisplayName("비팀원은 팀 프로젝트 조회가 불가능하다")
        void nonTeamMemberCannotAccessTeamProject() throws Exception {
            mockMvc.perform(get("/api/v1/teams/" + team.getId() + "/project")
                            .header("Authorization", "Bearer " + nonMemberToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("PROJECT_007"));
        }

        @Test
        @DisplayName("비팀원은 프로젝트 체크포인트 목록 조회가 불가능하다")
        void nonTeamMemberCannotAccessProjectCheckpoints() throws Exception {
            mockMvc.perform(get("/api/v1/projects/" + project.getId() + "/checkpoints")
                            .header("Authorization", "Bearer " + nonMemberToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("PROJECT_007"));
        }

        @Test
        @DisplayName("비팀원은 체크포인트 상세 조회가 불가능하다")
        void nonTeamMemberCannotAccessCheckpoint() throws Exception {
            mockMvc.perform(get("/api/v1/checkpoints/" + checkpoint.getId())
                            .header("Authorization", "Bearer " + nonMemberToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("PROJECT_007"));
        }
    }

    @Nested
    @DisplayName("팀원 사용자 접근 테스트")
    class TeamMemberAccessTest {

        private String leaderToken;

        @BeforeEach
        void setUpToken() {
            leaderToken = jwtTokenProvider.createToken(
                    teamLeader.getId(), teamLeader.getEmail(), teamLeader.getRole().name());
        }

        @Test
        @DisplayName("팀 리더는 프로젝트 상세 조회가 가능하다")
        void teamLeaderCanAccessProject() throws Exception {
            mockMvc.perform(get("/api/v1/projects/" + project.getId())
                            .header("Authorization", "Bearer " + leaderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(project.getId()));
        }

        @Test
        @DisplayName("팀 리더는 팀 프로젝트 조회가 가능하다")
        void teamLeaderCanAccessTeamProject() throws Exception {
            mockMvc.perform(get("/api/v1/teams/" + team.getId() + "/project")
                            .header("Authorization", "Bearer " + leaderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(project.getId()));
        }

        @Test
        @DisplayName("팀 리더는 프로젝트 체크포인트 목록 조회가 가능하다")
        void teamLeaderCanAccessProjectCheckpoints() throws Exception {
            mockMvc.perform(get("/api/v1/projects/" + project.getId() + "/checkpoints")
                            .header("Authorization", "Bearer " + leaderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("팀 리더는 체크포인트 상세 조회가 가능하다")
        void teamLeaderCanAccessCheckpoint() throws Exception {
            mockMvc.perform(get("/api/v1/checkpoints/" + checkpoint.getId())
                            .header("Authorization", "Bearer " + leaderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(checkpoint.getId()));
        }
    }
}
