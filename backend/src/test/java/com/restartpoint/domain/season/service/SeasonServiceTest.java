package com.restartpoint.domain.season.service;

import com.restartpoint.domain.season.dto.SeasonRequest;
import com.restartpoint.domain.season.dto.SeasonResponse;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.season.repository.SeasonRepository;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SeasonService seasonService;

    @Test
    @DisplayName("공개 시즌 상세 조회에서 DRAFT 시즌은 조회할 수 없다")
    void getSeasonFailsForDraftSeason() {
        Season draftSeason = createSeason(1L, SeasonStatus.DRAFT);
        given(seasonRepository.findById(1L)).willReturn(Optional.of(draftSeason));

        assertThatThrownBy(() -> seasonService.getSeason(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.SEASON_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("운영자 시즌 상세 조회에서는 DRAFT 시즌도 조회할 수 있다")
    void getSeasonForAdminReturnsDraftSeason() {
        Season draftSeason = createSeason(1L, SeasonStatus.DRAFT);
        given(seasonRepository.findById(1L)).willReturn(Optional.of(draftSeason));

        SeasonResponse response = seasonService.getSeasonForAdmin(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(SeasonStatus.DRAFT);
    }

    @Test
    @DisplayName("현재 참여 가능한 시즌 조회는 모집 중과 팀빌딩 중 시즌을 모두 반환한다")
    void getActiveSeasonsReturnsRecruitingAndTeamBuilding() {
        Season recruitingSeason = createSeason(1L, SeasonStatus.RECRUITING);
        Season teamBuildingSeason = createSeason(2L, SeasonStatus.TEAM_BUILDING);
        given(seasonRepository.findActiveSeasons()).willReturn(List.of(recruitingSeason, teamBuildingSeason));

        List<SeasonResponse> responses = seasonService.getActiveSeasons();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(SeasonResponse::getStatus)
                .containsExactly(SeasonStatus.RECRUITING, SeasonStatus.TEAM_BUILDING);
    }

    @Test
    @DisplayName("운영자 시즌 목록 조회는 DRAFT 시즌도 포함한다")
    void getAllSeasonsIncludesDraft() {
        Season draftSeason = createSeason(1L, SeasonStatus.DRAFT);
        Season publishedSeason = createSeason(2L, SeasonStatus.RECRUITING);
        given(seasonRepository.findAllByOrderByCreatedAtDesc(Pageable.ofSize(10)))
                .willReturn(new PageImpl<>(List.of(draftSeason, publishedSeason)));

        var responses = seasonService.getAllSeasons(Pageable.ofSize(10));

        assertThat(responses.getContent()).hasSize(2);
        assertThat(responses.getContent()).extracting(SeasonResponse::getStatus)
                .containsExactly(SeasonStatus.DRAFT, SeasonStatus.RECRUITING);
    }

    @Test
    @DisplayName("공개 시즌 목록 조회는 DRAFT 시즌을 제외한다")
    void getPublicSeasonsExcludesDraft() {
        Season recruitingSeason = createSeason(2L, SeasonStatus.RECRUITING);
        given(seasonRepository.findByStatusNotOrderByRecruitmentStartAtDesc(SeasonStatus.DRAFT))
                .willReturn(List.of(recruitingSeason));

        List<SeasonResponse> responses = seasonService.getPublicSeasons();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(SeasonStatus.RECRUITING);
    }

    private Season createSeason(Long id, SeasonStatus status) {
        LocalDateTime base = LocalDateTime.of(2026, 4, 1, 0, 0);
        Season season = Season.builder()
                .title("테스트 시즌")
                .description("설명")
                .recruitmentStartAt(base)
                .recruitmentEndAt(base.plusDays(7))
                .teamBuildingStartAt(base.plusDays(8))
                .teamBuildingEndAt(base.plusDays(14))
                .projectStartAt(base.plusDays(15))
                .projectEndAt(base.plusDays(30))
                .reviewStartAt(base.plusDays(31))
                .reviewEndAt(base.plusDays(37))
                .expertReviewWeight(100)
                .candidateReviewWeight(0)
                .build();
        setField(season, "id", id);
        setField(season, "status", status);
        return season;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("테스트 필드 설정에 실패했습니다: " + fieldName, exception);
        }
    }

    @Nested
    @DisplayName("사용자별 시즌 조회 테스트")
    class GetSeasonForUserTest {

        @Test
        @DisplayName("로그인 사용자 시즌 목록 조회 - 팀 리더인 경우 참여 정보가 포함된다")
        void getPublicSeasonsForUser_asTeamLeader_includesParticipationInfo() {
            // given
            User user = createUser(1L, "user@test.com");
            Season season = createSeason(1L, SeasonStatus.RECRUITING);
            Team team = createTeam(1L, "테스트 팀", season, user);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(seasonRepository.findByStatusNotOrderByRecruitmentStartAtDesc(SeasonStatus.DRAFT))
                    .willReturn(List.of(season));
            given(teamRepository.findByLeader(user)).willReturn(List.of(team));
            given(teamMemberRepository.findByUserAndStatus(user, TeamMemberStatus.ACCEPTED))
                    .willReturn(Collections.emptyList());

            // when
            List<SeasonResponse> responses = seasonService.getPublicSeasonsForUser(1L);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getMyTeamId()).isEqualTo(1L);
            assertThat(responses.get(0).getMyTeamName()).isEqualTo("테스트 팀");
        }

        @Test
        @DisplayName("로그인 사용자 시즌 목록 조회 - 팀원인 경우 참여 정보가 포함된다")
        void getPublicSeasonsForUser_asTeamMember_includesParticipationInfo() {
            // given
            User user = createUser(1L, "user@test.com");
            User leader = createUser(2L, "leader@test.com");
            Season season = createSeason(1L, SeasonStatus.RECRUITING);
            Team team = createTeam(1L, "테스트 팀", season, leader);
            TeamMember teamMember = createTeamMember(1L, team, user);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(seasonRepository.findByStatusNotOrderByRecruitmentStartAtDesc(SeasonStatus.DRAFT))
                    .willReturn(List.of(season));
            given(teamRepository.findByLeader(user)).willReturn(Collections.emptyList());
            given(teamMemberRepository.findByUserAndStatus(user, TeamMemberStatus.ACCEPTED))
                    .willReturn(List.of(teamMember));

            // when
            List<SeasonResponse> responses = seasonService.getPublicSeasonsForUser(1L);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getMyTeamId()).isEqualTo(1L);
            assertThat(responses.get(0).getMyTeamName()).isEqualTo("테스트 팀");
        }

        @Test
        @DisplayName("로그인 사용자 시즌 목록 조회 - 참여 팀이 없는 경우 참여 정보가 null이다")
        void getPublicSeasonsForUser_noTeam_participationInfoIsNull() {
            // given
            User user = createUser(1L, "user@test.com");
            Season season = createSeason(1L, SeasonStatus.RECRUITING);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(seasonRepository.findByStatusNotOrderByRecruitmentStartAtDesc(SeasonStatus.DRAFT))
                    .willReturn(List.of(season));
            given(teamRepository.findByLeader(user)).willReturn(Collections.emptyList());
            given(teamMemberRepository.findByUserAndStatus(user, TeamMemberStatus.ACCEPTED))
                    .willReturn(Collections.emptyList());

            // when
            List<SeasonResponse> responses = seasonService.getPublicSeasonsForUser(1L);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getMyTeamId()).isNull();
            assertThat(responses.get(0).getMyTeamName()).isNull();
        }

        @Test
        @DisplayName("로그인 사용자 시즌 상세 조회 - 팀 리더인 경우 참여 정보가 포함된다")
        void getSeasonForUser_asTeamLeader_includesParticipationInfo() {
            // given
            User user = createUser(1L, "user@test.com");
            Season season = createSeason(1L, SeasonStatus.RECRUITING);
            Team team = createTeam(1L, "테스트 팀", season, user);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(seasonRepository.findById(1L)).willReturn(Optional.of(season));
            given(teamRepository.findByLeader(user)).willReturn(List.of(team));

            // when
            SeasonResponse response = seasonService.getSeasonForUser(1L, 1L);

            // then
            assertThat(response.getMyTeamId()).isEqualTo(1L);
            assertThat(response.getMyTeamName()).isEqualTo("테스트 팀");
        }

        @Test
        @DisplayName("로그인 사용자 시즌 상세 조회 - 팀원인 경우 참여 정보가 포함된다")
        void getSeasonForUser_asTeamMember_includesParticipationInfo() {
            // given
            User user = createUser(1L, "user@test.com");
            User leader = createUser(2L, "leader@test.com");
            Season season = createSeason(1L, SeasonStatus.RECRUITING);
            Team team = createTeam(1L, "테스트 팀", season, leader);
            TeamMember teamMember = createTeamMember(1L, team, user);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(seasonRepository.findById(1L)).willReturn(Optional.of(season));
            given(teamRepository.findByLeader(user)).willReturn(Collections.emptyList());
            given(teamMemberRepository.findByUserAndStatus(user, TeamMemberStatus.ACCEPTED))
                    .willReturn(List.of(teamMember));

            // when
            SeasonResponse response = seasonService.getSeasonForUser(1L, 1L);

            // then
            assertThat(response.getMyTeamId()).isEqualTo(1L);
            assertThat(response.getMyTeamName()).isEqualTo("테스트 팀");
        }

        @Test
        @DisplayName("로그인 사용자 시즌 상세 조회 - 참여 팀이 없는 경우 참여 정보가 null이다")
        void getSeasonForUser_noTeam_participationInfoIsNull() {
            // given
            User user = createUser(1L, "user@test.com");
            Season season = createSeason(1L, SeasonStatus.RECRUITING);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(seasonRepository.findById(1L)).willReturn(Optional.of(season));
            given(teamRepository.findByLeader(user)).willReturn(Collections.emptyList());
            given(teamMemberRepository.findByUserAndStatus(user, TeamMemberStatus.ACCEPTED))
                    .willReturn(Collections.emptyList());

            // when
            SeasonResponse response = seasonService.getSeasonForUser(1L, 1L);

            // then
            assertThat(response.getMyTeamId()).isNull();
            assertThat(response.getMyTeamName()).isNull();
        }

        @Test
        @DisplayName("로그인 사용자 시즌 상세 조회 - DRAFT 시즌은 조회할 수 없다")
        void getSeasonForUser_draftSeason_throwsException() {
            // given
            User user = createUser(1L, "user@test.com");
            Season season = createSeason(1L, SeasonStatus.DRAFT);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(seasonRepository.findById(1L)).willReturn(Optional.of(season));

            // when & then
            assertThatThrownBy(() -> seasonService.getSeasonForUser(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.SEASON_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("로그인 사용자 시즌 조회 - 존재하지 않는 사용자인 경우 예외가 발생한다")
        void getSeasonForUser_userNotFound_throwsException() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> seasonService.getSeasonForUser(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("로그인 사용자 시즌 목록 조회 - 여러 시즌에 각각 다른 팀으로 참여하는 경우")
        void getPublicSeasonsForUser_multipleSeasons_eachWithDifferentTeam() {
            // given
            User user = createUser(1L, "user@test.com");
            Season season1 = createSeason(1L, SeasonStatus.RECRUITING);
            Season season2 = createSeason(2L, SeasonStatus.IN_PROGRESS);
            Team team1 = createTeam(1L, "시즌1 팀", season1, user);
            Team team2 = createTeam(2L, "시즌2 팀", season2, user);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(seasonRepository.findByStatusNotOrderByRecruitmentStartAtDesc(SeasonStatus.DRAFT))
                    .willReturn(List.of(season1, season2));
            given(teamRepository.findByLeader(user)).willReturn(List.of(team1, team2));
            given(teamMemberRepository.findByUserAndStatus(user, TeamMemberStatus.ACCEPTED))
                    .willReturn(Collections.emptyList());

            // when
            List<SeasonResponse> responses = seasonService.getPublicSeasonsForUser(1L);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getMyTeamId()).isEqualTo(1L);
            assertThat(responses.get(0).getMyTeamName()).isEqualTo("시즌1 팀");
            assertThat(responses.get(1).getMyTeamId()).isEqualTo(2L);
            assertThat(responses.get(1).getMyTeamName()).isEqualTo("시즌2 팀");
        }

        private User createUser(Long id, String email) {
            User user = User.builder()
                    .email(email)
                    .password("password")
                    .name("테스트 사용자")
                    .role(Role.USER)
                    .build();
            setField(user, "id", id);
            return user;
        }

        private Team createTeam(Long id, String name, Season season, User leader) {
            Team team = Team.builder()
                    .name(name)
                    .description("테스트 팀 설명")
                    .season(season)
                    .leader(leader)
                    .build();
            setField(team, "id", id);
            return team;
        }

        private TeamMember createTeamMember(Long id, Team team, User user) {
            TeamMember teamMember = TeamMember.builder()
                    .team(team)
                    .user(user)
                    .build();
            setField(teamMember, "id", id);
            setField(teamMember, "status", TeamMemberStatus.ACCEPTED);
            return teamMember;
        }
    }
}
