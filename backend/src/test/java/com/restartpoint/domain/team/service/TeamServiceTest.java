package com.restartpoint.domain.team.service;

import com.restartpoint.domain.notification.service.NotificationService;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.season.repository.SeasonRepository;
import com.restartpoint.domain.team.dto.TeamApplyRequest;
import com.restartpoint.domain.team.dto.TeamMemberResponse;
import com.restartpoint.domain.team.dto.TeamRequest;
import com.restartpoint.domain.team.dto.TeamResponse;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.entity.TeamMember;
import com.restartpoint.domain.team.entity.TeamMemberStatus;
import com.restartpoint.domain.team.entity.TeamStatus;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.team.repository.TeamRepository;
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
class TeamServiceTest {

  @Mock
  private TeamRepository teamRepository;

  @Mock
  private TeamMemberRepository teamMemberRepository;

  @Mock
  private SeasonRepository seasonRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private TeamService teamService;

  @Nested
  @DisplayName("팀 생성")
  class CreateTeam {

    @Test
    @DisplayName("수료 인증된 사용자가 팀빌딩 기간에 팀을 생성할 수 있다")
    void createTeamSuccess() {
      // given
      User certifiedUser = createCertifiedUser(1L, "user@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      TeamRequest request = createTeamRequest(1L, "팀이름", "팀설명", JobRole.BACKEND);

      given(userRepository.findById(1L)).willReturn(Optional.of(certifiedUser));
      given(seasonRepository.findById(1L)).willReturn(Optional.of(season));
      given(teamRepository.findByLeader(certifiedUser)).willReturn(List.of());
      given(teamMemberRepository.existsAcceptedMemberInSeason(certifiedUser, season)).willReturn(false);
      given(teamRepository.save(any(Team.class))).willAnswer(invocation -> {
        Team team = invocation.getArgument(0);
        setField(team, "id", 1L);
        return team;
      });
      given(teamMemberRepository.save(any(TeamMember.class))).willAnswer(invocation -> {
        TeamMember member = invocation.getArgument(0);
        setField(member, "id", 1L);
        return member;
      });

      // when
      TeamResponse response = teamService.createTeam(1L, request);

      // then
      assertThat(response).isNotNull();
      assertThat(response.getName()).isEqualTo("팀이름");
      verify(teamRepository).save(any(Team.class));
      verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("수료 인증되지 않은 사용자는 팀을 생성할 수 없다")
    void createTeamFailsWhenNotCertified() {
      // given
      User uncertifiedUser = createUser(1L, "user@example.com", "사용자");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      TeamRequest request = createTeamRequest(1L, "팀이름", "팀설명", JobRole.BACKEND);

      given(userRepository.findById(1L)).willReturn(Optional.of(uncertifiedUser));
      given(seasonRepository.findById(1L)).willReturn(Optional.of(season));

      // when & then
      assertThatThrownBy(() -> teamService.createTeam(1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.CERTIFICATION_REQUIRED);

      verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("수료 인증된 사용자가 모집 기간에 팀을 생성할 수 있다")
    void createTeamSuccessInRecruitingPeriod() {
      // given
      User certifiedUser = createCertifiedUser(1L, "user@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.RECRUITING);
      TeamRequest request = createTeamRequest(1L, "팀이름", "팀설명", JobRole.BACKEND);

      given(userRepository.findById(1L)).willReturn(Optional.of(certifiedUser));
      given(seasonRepository.findById(1L)).willReturn(Optional.of(season));
      given(teamRepository.findByLeader(certifiedUser)).willReturn(List.of());
      given(teamMemberRepository.existsAcceptedMemberInSeason(certifiedUser, season)).willReturn(false);
      given(teamRepository.save(any(Team.class))).willAnswer(invocation -> {
        Team team = invocation.getArgument(0);
        setField(team, "id", 1L);
        return team;
      });
      given(teamMemberRepository.save(any(TeamMember.class))).willAnswer(invocation -> {
        TeamMember member = invocation.getArgument(0);
        setField(member, "id", 1L);
        return member;
      });

      // when
      TeamResponse response = teamService.createTeam(1L, request);

      // then
      assertThat(response).isNotNull();
      assertThat(response.getName()).isEqualTo("팀이름");
      verify(teamRepository).save(any(Team.class));
      verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("진행 중인 시즌에서는 팀을 생성할 수 없다")
    void createTeamFailsWhenSeasonInProgress() {
      // given
      User certifiedUser = createCertifiedUser(1L, "user@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.IN_PROGRESS);
      TeamRequest request = createTeamRequest(1L, "팀이름", "팀설명", JobRole.BACKEND);

      given(userRepository.findById(1L)).willReturn(Optional.of(certifiedUser));
      given(seasonRepository.findById(1L)).willReturn(Optional.of(season));

      // when & then
      assertThatThrownBy(() -> teamService.createTeam(1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SEASON_STATUS);

      verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("심사 중인 시즌에서는 팀을 생성할 수 없다")
    void createTeamFailsWhenSeasonReviewing() {
      // given
      User certifiedUser = createCertifiedUser(1L, "user@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.REVIEWING);
      TeamRequest request = createTeamRequest(1L, "팀이름", "팀설명", JobRole.BACKEND);

      given(userRepository.findById(1L)).willReturn(Optional.of(certifiedUser));
      given(seasonRepository.findById(1L)).willReturn(Optional.of(season));

      // when & then
      assertThatThrownBy(() -> teamService.createTeam(1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SEASON_STATUS);

      verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("완료된 시즌에서는 팀을 생성할 수 없다")
    void createTeamFailsWhenSeasonCompleted() {
      // given
      User certifiedUser = createCertifiedUser(1L, "user@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.COMPLETED);
      TeamRequest request = createTeamRequest(1L, "팀이름", "팀설명", JobRole.BACKEND);

      given(userRepository.findById(1L)).willReturn(Optional.of(certifiedUser));
      given(seasonRepository.findById(1L)).willReturn(Optional.of(season));

      // when & then
      assertThatThrownBy(() -> teamService.createTeam(1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SEASON_STATUS);

      verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("이미 해당 시즌에서 팀에 소속된 사용자는 팀을 생성할 수 없다")
    void createTeamFailsWhenAlreadyInTeam() {
      // given
      User certifiedUser = createCertifiedUser(1L, "user@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team existingTeam = createTeam(1L, "기존팀", season, certifiedUser);
      TeamRequest request = createTeamRequest(1L, "새팀", "팀설명", JobRole.BACKEND);

      given(userRepository.findById(1L)).willReturn(Optional.of(certifiedUser));
      given(seasonRepository.findById(1L)).willReturn(Optional.of(season));
      given(teamRepository.findByLeader(certifiedUser)).willReturn(List.of(existingTeam));

      // when & then
      assertThatThrownBy(() -> teamService.createTeam(1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ALREADY_IN_TEAM);

      verify(teamRepository, never()).save(any(Team.class));
    }
  }

  @Nested
  @DisplayName("팀 조회")
  class GetTeam {

    @Test
    @DisplayName("팀 상세 정보를 조회할 수 있다")
    void getTeamSuccess() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);

      given(teamRepository.findById(1L)).willReturn(Optional.of(team));

      // when
      TeamResponse response = teamService.getTeam(1L);

      // then
      assertThat(response).isNotNull();
      assertThat(response.getName()).isEqualTo("팀이름");
    }

    @Test
    @DisplayName("존재하지 않는 팀 조회 시 예외가 발생한다")
    void getTeamFailsWhenNotFound() {
      // given
      given(teamRepository.findById(999L)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> teamService.getTeam(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    @DisplayName("시즌별 팀 목록을 조회할 수 있다")
    void getTeamsBySeasonSuccess() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team1 = createTeam(1L, "팀1", season, leader);
      Team team2 = createTeam(2L, "팀2", season, leader);

      given(seasonRepository.findById(1L)).willReturn(Optional.of(season));
      given(teamRepository.findBySeason(season)).willReturn(List.of(team1, team2));

      // when
      List<TeamResponse> responses = teamService.getTeamsBySeason(1L);

      // then
      assertThat(responses).hasSize(2);
    }
  }

  @Nested
  @DisplayName("팀 지원")
  class ApplyToTeam {

    @Test
    @DisplayName("수료 인증된 사용자가 모집 중인 팀에 지원할 수 있다")
    void applyToTeamSuccess() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      User applicant = createCertifiedUser(2L, "applicant@example.com", "지원자");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeamWithRecruiting(1L, "팀이름", season, leader, true, false, false, true);
      TeamApplyRequest request = createApplyRequest(JobRole.BACKEND, "지원합니다");

      given(userRepository.findById(2L)).willReturn(Optional.of(applicant));
      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(teamRepository.findByLeader(applicant)).willReturn(List.of());
      given(teamMemberRepository.existsAcceptedMemberInSeason(applicant, season)).willReturn(false);
      given(teamMemberRepository.existsByTeamAndUser(team, applicant)).willReturn(false);
      given(teamMemberRepository.save(any(TeamMember.class))).willAnswer(invocation -> {
        TeamMember member = invocation.getArgument(0);
        setField(member, "id", 1L);
        return member;
      });

      // when
      TeamMemberResponse response = teamService.applyToTeam(2L, 1L, request);

      // then
      assertThat(response).isNotNull();
      assertThat(response.getRole()).isEqualTo(JobRole.BACKEND);
      verify(teamMemberRepository).save(any(TeamMember.class));
      verify(notificationService).notifyTeamApplication(1L, "지원자", "팀이름", 1L);
    }

    @Test
    @DisplayName("수료 인증되지 않은 사용자는 팀에 지원할 수 없다")
    void applyToTeamFailsWhenNotCertified() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      User uncertifiedUser = createUser(2L, "user@example.com", "사용자");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamApplyRequest request = createApplyRequest(JobRole.BACKEND, "지원합니다");

      given(userRepository.findById(2L)).willReturn(Optional.of(uncertifiedUser));
      given(teamRepository.findById(1L)).willReturn(Optional.of(team));

      // when & then
      assertThatThrownBy(() -> teamService.applyToTeam(2L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.CERTIFICATION_REQUIRED);

      verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("모집이 완료된 팀에는 지원할 수 없다")
    void applyToTeamFailsWhenNotRecruiting() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      User applicant = createCertifiedUser(2L, "applicant@example.com", "지원자");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      team.updateStatus(TeamStatus.COMPLETE);
      TeamApplyRequest request = createApplyRequest(JobRole.BACKEND, "지원합니다");

      given(userRepository.findById(2L)).willReturn(Optional.of(applicant));
      given(teamRepository.findById(1L)).willReturn(Optional.of(team));

      // when & then
      assertThatThrownBy(() -> teamService.applyToTeam(2L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SEASON_NOT_RECRUITING);

      verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("이미 지원한 팀에 중복 지원할 수 없다")
    void applyToTeamFailsWhenAlreadyApplied() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      User applicant = createCertifiedUser(2L, "applicant@example.com", "지원자");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeamWithRecruiting(1L, "팀이름", season, leader, true, false, false, true);
      TeamApplyRequest request = createApplyRequest(JobRole.BACKEND, "지원합니다");

      given(userRepository.findById(2L)).willReturn(Optional.of(applicant));
      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(teamRepository.findByLeader(applicant)).willReturn(List.of());
      given(teamMemberRepository.existsAcceptedMemberInSeason(applicant, season)).willReturn(false);
      given(teamMemberRepository.existsByTeamAndUser(team, applicant)).willReturn(true);

      // when & then
      assertThatThrownBy(() -> teamService.applyToTeam(2L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ALREADY_APPLIED);

      verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("모집 중이 아닌 역할로 지원하면 예외가 발생한다")
    void applyToTeamFailsWhenRoleNotRecruiting() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      User applicant = createCertifiedUser(2L, "applicant@example.com", "지원자");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeamWithRecruiting(1L, "팀이름", season, leader, false, false, true, false);
      TeamApplyRequest request = createApplyRequest(JobRole.BACKEND, "지원합니다"); // BACKEND는 모집 안함

      given(userRepository.findById(2L)).willReturn(Optional.of(applicant));
      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(teamRepository.findByLeader(applicant)).willReturn(List.of());
      given(teamMemberRepository.existsAcceptedMemberInSeason(applicant, season)).willReturn(false);
      given(teamMemberRepository.existsByTeamAndUser(team, applicant)).willReturn(false);

      // when & then
      assertThatThrownBy(() -> teamService.applyToTeam(2L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

      verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }
  }

  @Nested
  @DisplayName("팀 지원 수락/거절")
  class AcceptRejectApplication {

    @Test
    @DisplayName("팀 리더가 지원을 수락할 수 있다")
    void acceptApplicationSuccess() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      User applicant = createCertifiedUser(2L, "applicant@example.com", "지원자");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamMember pendingMember = createTeamMember(1L, team, applicant, JobRole.BACKEND, TeamMemberStatus.PENDING);

      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(teamMemberRepository.findById(1L)).willReturn(Optional.of(pendingMember));
      given(teamRepository.findByLeader(applicant)).willReturn(List.of());
      given(teamMemberRepository.existsAcceptedMemberInSeason(applicant, season)).willReturn(false);

      // when
      TeamMemberResponse response = teamService.acceptApplication(1L, 1L, 1L);

      // then
      assertThat(response.getStatus()).isEqualTo(TeamMemberStatus.ACCEPTED);
      verify(notificationService).notifyTeamInvitation(2L, "팀이름", 1L);
    }

    @Test
    @DisplayName("팀 리더가 아닌 사용자는 지원을 수락할 수 없다")
    void acceptApplicationFailsWhenNotLeader() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      User otherUser = createCertifiedUser(3L, "other@example.com", "다른사람");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);

      given(teamRepository.findById(1L)).willReturn(Optional.of(team));

      // when & then
      assertThatThrownBy(() -> teamService.acceptApplication(3L, 1L, 1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.NOT_TEAM_LEADER);
    }

    @Test
    @DisplayName("팀 리더가 지원을 거절할 수 있다")
    void rejectApplicationSuccess() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      User applicant = createCertifiedUser(2L, "applicant@example.com", "지원자");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamMember pendingMember = createTeamMember(1L, team, applicant, JobRole.BACKEND, TeamMemberStatus.PENDING);

      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(teamMemberRepository.findById(1L)).willReturn(Optional.of(pendingMember));

      // when
      TeamMemberResponse response = teamService.rejectApplication(1L, 1L, 1L);

      // then
      assertThat(response.getStatus()).isEqualTo(TeamMemberStatus.REJECTED);
      verify(notificationService).notifyTeamApplicationRejected(2L, "팀이름");
    }

    @Test
    @DisplayName("이미 처리된 지원은 수락할 수 없다")
    void acceptApplicationFailsWhenAlreadyProcessed() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      User applicant = createCertifiedUser(2L, "applicant@example.com", "지원자");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamMember acceptedMember = createTeamMember(1L, team, applicant, JobRole.BACKEND, TeamMemberStatus.ACCEPTED);

      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(teamMemberRepository.findById(1L)).willReturn(Optional.of(acceptedMember));

      // when & then
      assertThatThrownBy(() -> teamService.acceptApplication(1L, 1L, 1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.APPLICATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("팀 탈퇴")
  class LeaveTeam {

    @Test
    @DisplayName("팀 멤버가 팀에서 탈퇴할 수 있다")
    void leaveTeamSuccess() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      User member = createCertifiedUser(2L, "member@example.com", "팀원");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamMember teamMember = createTeamMember(1L, team, member, JobRole.BACKEND, TeamMemberStatus.ACCEPTED);

      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(userRepository.findById(2L)).willReturn(Optional.of(member));
      given(teamMemberRepository.findByTeamAndUser(team, member)).willReturn(Optional.of(teamMember));

      // when
      teamService.leaveTeam(2L, 1L);

      // then
      verify(teamMemberRepository).delete(teamMember);
    }

    @Test
    @DisplayName("팀 리더는 탈퇴할 수 없다")
    void leaveTeamFailsWhenLeader() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);

      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(userRepository.findById(1L)).willReturn(Optional.of(leader));

      // when & then
      assertThatThrownBy(() -> teamService.leaveTeam(1L, 1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.NOT_TEAM_LEADER);

      verify(teamMemberRepository, never()).delete(any(TeamMember.class));
    }
  }

  @Nested
  @DisplayName("팀 정보 수정")
  class UpdateTeam {

    @Test
    @DisplayName("팀 리더가 팀 정보를 수정할 수 있다")
    void updateTeamSuccess() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamRequest request = createTeamRequest(1L, "수정된팀", "수정된설명", JobRole.BACKEND);

      given(teamRepository.findById(1L)).willReturn(Optional.of(team));

      // when
      TeamResponse response = teamService.updateTeam(1L, 1L, request);

      // then
      assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("팀 리더가 아닌 사용자는 팀 정보를 수정할 수 없다")
    void updateTeamFailsWhenNotLeader() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "팀장");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamRequest request = createTeamRequest(1L, "수정된팀", "수정된설명", JobRole.BACKEND);

      given(teamRepository.findById(1L)).willReturn(Optional.of(team));

      // when & then
      assertThatThrownBy(() -> teamService.updateTeam(2L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.NOT_TEAM_LEADER);
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

  private Season createSeason(Long id, String title, SeasonStatus status) {
    LocalDateTime now = LocalDateTime.now();
    Season season = Season.builder()
        .title(title)
        .description("시즌 설명")
        .recruitmentStartAt(now.minusDays(30))
        .recruitmentEndAt(now.minusDays(20))
        .teamBuildingStartAt(now.minusDays(10))
        .teamBuildingEndAt(now.plusDays(10))
        .projectStartAt(now.plusDays(20))
        .projectEndAt(now.plusDays(50))
        .reviewStartAt(now.plusDays(60))
        .reviewEndAt(now.plusDays(70))
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
        .recruitingBackend(true)
        .recruitingFrontend(true)
        .build();
    setField(team, "id", id);
    return team;
  }

  private Team createTeamWithRecruiting(Long id, String name, Season season, User leader,
      Boolean planner, Boolean uxui, Boolean frontend, Boolean backend) {
    Team team = Team.builder()
        .name(name)
        .description("팀 설명")
        .season(season)
        .leader(leader)
        .recruitingPlanner(planner)
        .recruitingUxui(uxui)
        .recruitingFrontend(frontend)
        .recruitingBackend(backend)
        .build();
    setField(team, "id", id);
    return team;
  }

  private TeamMember createTeamMember(Long id, Team team, User user, JobRole role, TeamMemberStatus status) {
    TeamMember member = TeamMember.builder()
        .team(team)
        .user(user)
        .role(role)
        .applicationMessage("지원 메시지")
        .build();
    setField(member, "id", id);
    if (status == TeamMemberStatus.ACCEPTED) {
      member.accept();
    } else if (status == TeamMemberStatus.REJECTED) {
      member.reject();
    }
    return member;
  }

  private TeamRequest createTeamRequest(Long seasonId, String name, String description, JobRole leaderRole) {
    TeamRequest request = new TeamRequest();
    setField(request, "seasonId", seasonId);
    setField(request, "name", name);
    setField(request, "description", description);
    setField(request, "leaderRole", leaderRole);
    setField(request, "recruitingBackend", true);
    setField(request, "recruitingFrontend", true);
    return request;
  }

  private TeamApplyRequest createApplyRequest(JobRole role, String message) {
    TeamApplyRequest request = new TeamApplyRequest();
    setField(request, "role", role);
    setField(request, "applicationMessage", message);
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
