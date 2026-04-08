package com.restartpoint.domain.matching.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restartpoint.domain.matching.dto.MemberRecommendationResponse;
import com.restartpoint.domain.matching.dto.TeamRecommendationResponse;
import com.restartpoint.domain.profile.entity.CollaborationStyle;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.profile.entity.Profile;
import com.restartpoint.domain.profile.entity.ProjectDifficulty;
import com.restartpoint.domain.profile.repository.ProfileRepository;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.entity.TeamMember;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.team.repository.TeamRepository;
import com.restartpoint.domain.user.entity.CertificationStatus;
import com.restartpoint.domain.user.entity.Role;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import com.restartpoint.infra.ai.GroqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TeamMatchingServiceTest {

    @Mock
    private GroqService groqService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TeamMatchingService teamMatchingService;

    private User testUser;
    private Profile testProfile;
    private Season testSeason;
    private Team testTeam;

    @BeforeEach
    void setUp() {
        testUser = createUser(1L, "user@example.com", "테스트유저");
        testProfile = createProfile(1L, testUser, JobRole.BACKEND, List.of("Java", "Spring"));
        testSeason = createSeason(1L, "시즌 1", SeasonStatus.TEAM_BUILDING);
        testTeam = createTeam(1L, "테스트 팀", testSeason, createUser(2L, "leader@example.com", "리더"));
    }

    @Test
    @DisplayName("팀 추천 시 사용자가 존재하지 않으면 예외가 발생한다")
    void recommendTeamsFailsWhenUserNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamMatchingService.recommendTeamsForUser(99L, 1L, 5))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("팀 추천 시 프로필이 없으면 예외가 발생한다")
    void recommendTeamsFailsWhenProfileNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(profileRepository.findByUser(testUser)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamMatchingService.recommendTeamsForUser(1L, 1L, 5))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PROFILE_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("팀 추천 시 시즌이 존재하지 않으면 예외가 발생한다")
    void recommendTeamsFailsWhenSeasonNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(profileRepository.findByUser(testUser)).willReturn(Optional.of(testProfile));
        given(seasonRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamMatchingService.recommendTeamsForUser(1L, 99L, 5))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.SEASON_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("이미 팀에 소속된 사용자가 팀 추천을 요청하면 예외가 발생한다")
    void recommendTeamsFailsWhenAlreadyInTeam() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(profileRepository.findByUser(testUser)).willReturn(Optional.of(testProfile));
        given(seasonRepository.findById(1L)).willReturn(Optional.of(testSeason));
        given(teamRepository.findByLeader(testUser)).willReturn(List.of(testTeam));

        assertThatThrownBy(() -> teamMatchingService.recommendTeamsForUser(1L, 1L, 5))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ALREADY_IN_TEAM);
                });
    }

    @Test
    @DisplayName("모집 중인 팀이 없으면 예외가 발생한다")
    void recommendTeamsFailsWhenNoRecruitingTeams() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(profileRepository.findByUser(testUser)).willReturn(Optional.of(testProfile));
        given(seasonRepository.findById(1L)).willReturn(Optional.of(testSeason));
        given(teamRepository.findByLeader(testUser)).willReturn(List.of());
        given(teamMemberRepository.existsAcceptedMemberInSeason(testUser, testSeason)).willReturn(false);
        given(teamRepository.findRecruitingTeamsBySeason(testSeason)).willReturn(List.of());

        assertThatThrownBy(() -> teamMatchingService.recommendTeamsForUser(1L, 1L, 5))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NO_MATCHING_CANDIDATES);
                });
    }

    @Test
    @DisplayName("AI 응답이 없을 때 기본 추천을 반환한다")
    void recommendTeamsReturnsDefaultWhenAIFails() {
        User leader = createUser(2L, "leader@example.com", "리더");
        Team recruitingTeam = createTeam(1L, "모집팀", testSeason, leader);
        setField(recruitingTeam, "recruitingBackend", true);

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(profileRepository.findByUser(testUser)).willReturn(Optional.of(testProfile));
        given(seasonRepository.findById(1L)).willReturn(Optional.of(testSeason));
        given(teamRepository.findByLeader(testUser)).willReturn(List.of());
        given(teamMemberRepository.existsAcceptedMemberInSeason(testUser, testSeason)).willReturn(false);
        given(teamRepository.findRecruitingTeamsBySeason(testSeason)).willReturn(List.of(recruitingTeam));
        given(groqService.chat(anyString(), anyString())).willReturn(null);

        List<TeamRecommendationResponse> result = teamMatchingService.recommendTeamsForUser(1L, 1L, 5);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getMatchScore()).isEqualTo(70);
        assertThat(result.get(0).getReasons()).hasSize(3);
    }

    @Test
    @DisplayName("멤버 추천 시 팀 리더가 아니면 예외가 발생한다")
    void recommendMembersFailsWhenNotLeader() {
        User anotherUser = createUser(3L, "another@example.com", "다른유저");
        given(userRepository.findById(3L)).willReturn(Optional.of(anotherUser));
        given(teamRepository.findById(1L)).willReturn(Optional.of(testTeam));

        assertThatThrownBy(() -> teamMatchingService.recommendMembersForTeam(3L, 1L, 5))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_TEAM_LEADER);
                });
    }

    @Test
    @DisplayName("팀이 가득 찼을 때 멤버 추천을 요청하면 예외가 발생한다")
    void recommendMembersFailsWhenTeamFull() {
        User leader = createUser(2L, "leader@example.com", "리더");
        Team fullTeam = createTeam(1L, "가득 찬 팀", testSeason, leader);
        // 4명의 멤버 추가 (팀 가득 참)
        for (int i = 0; i < 4; i++) {
            TeamMember member = createTeamMember(fullTeam, createUser((long)(10+i), "member"+i+"@example.com", "멤버"+i), JobRole.BACKEND);
            fullTeam.addMember(member);
        }

        given(userRepository.findById(2L)).willReturn(Optional.of(leader));
        given(teamRepository.findById(1L)).willReturn(Optional.of(fullTeam));

        assertThatThrownBy(() -> teamMatchingService.recommendMembersForTeam(2L, 1L, 5))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.TEAM_FULL);
                });
    }

    // 헬퍼 메서드들
    private User createUser(Long id, String email, String name) {
        User user = User.builder()
                .email(email)
                .password("encoded-password")
                .name(name)
                .role(Role.USER)
                .build();
        setField(user, "id", id);
        setField(user, "certificationStatus", CertificationStatus.APPROVED);
        return user;
    }

    private Profile createProfile(Long id, User user, JobRole jobRole, List<String> techStacks) {
        Profile profile = Profile.builder()
                .user(user)
                .jobRole(jobRole)
                .techStacks(techStacks)
                .interestedDomains(List.of("AI/ML"))
                .availableHoursPerWeek(20)
                .collaborationStyle(CollaborationStyle.COLLABORATIVE)
                .preferredDifficulty(ProjectDifficulty.INTERMEDIATE)
                .build();
        setField(profile, "id", id);
        return profile;
    }

    private Season createSeason(Long id, String title, SeasonStatus status) {
        Season season = Season.builder()
                .title(title)
                .description("테스트 시즌")
                .recruitmentStartAt(LocalDateTime.now().minusDays(10))
                .recruitmentEndAt(LocalDateTime.now().minusDays(5))
                .teamBuildingStartAt(LocalDateTime.now().minusDays(3))
                .teamBuildingEndAt(LocalDateTime.now().plusDays(7))
                .projectStartAt(LocalDateTime.now().plusDays(10))
                .projectEndAt(LocalDateTime.now().plusDays(40))
                .reviewStartAt(LocalDateTime.now().plusDays(42))
                .reviewEndAt(LocalDateTime.now().plusDays(50))
                .expertReviewWeight(70)
                .candidateReviewWeight(30)
                .build();
        setField(season, "id", id);
        setField(season, "status", status);
        return season;
    }

    private Team createTeam(Long id, String name, Season season, User leader) {
        Team team = Team.builder()
                .name(name)
                .description("테스트 팀 설명")
                .season(season)
                .leader(leader)
                .recruitingPlanner(false)
                .recruitingUxui(false)
                .recruitingFrontend(false)
                .recruitingBackend(false)
                .build();
        setField(team, "id", id);
        return team;
    }

    private TeamMember createTeamMember(Team team, User user, JobRole role) {
        TeamMember member = TeamMember.builder()
                .team(team)
                .user(user)
                .role(role)
                .build();
        member.accept();
        return member;
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
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), fieldName);
            }
            throw e;
        }
    }
}
