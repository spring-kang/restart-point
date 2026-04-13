package com.restartpoint.domain.team.service;

import com.restartpoint.domain.notification.service.NotificationService;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.season.entity.SeasonStatus;
import com.restartpoint.domain.team.dto.TeamInvitationRequest;
import com.restartpoint.domain.team.dto.TeamInvitationResponse;
import com.restartpoint.domain.team.entity.*;
import com.restartpoint.domain.team.repository.TeamInvitationRepository;
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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TeamInvitationServiceTest {

  @Mock
  private TeamInvitationRepository invitationRepository;

  @Mock
  private TeamRepository teamRepository;

  @Mock
  private TeamMemberRepository teamMemberRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private TeamInvitationService invitationService;

  @Nested
  @DisplayName("영입 요청 발송")
  class SendInvitation {

    @Test
    @DisplayName("팀 리더가 다른 사용자에게 영입 요청을 보낼 수 있다")
    void sendInvitationSuccess() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeamWithRecruiting(1L, "팀이름", season, leader, true, false, false, true);
      TeamInvitationRequest request = createInvitationRequest(2L, JobRole.BACKEND, "함께해요!", 85);

      given(userRepository.findById(1L)).willReturn(Optional.of(leader));
      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(userRepository.findById(2L)).willReturn(Optional.of(invitedUser));
      given(invitationRepository.existsValidPendingInvitation(eq(team), eq(invitedUser), any())).willReturn(false);
      given(teamMemberRepository.existsByTeamAndUser(team, invitedUser)).willReturn(false);
      given(teamRepository.findByLeader(invitedUser)).willReturn(List.of());
      given(teamMemberRepository.existsAcceptedMemberInSeason(invitedUser, season)).willReturn(false);
      given(invitationRepository.save(any(TeamInvitation.class))).willAnswer(invocation -> {
        TeamInvitation invitation = invocation.getArgument(0);
        setField(invitation, "id", 1L);
        return invitation;
      });

      // when
      TeamInvitationResponse response = invitationService.sendInvitation(1L, 1L, request);

      // then
      assertThat(response).isNotNull();
      assertThat(response.getSuggestedRole()).isEqualTo(JobRole.BACKEND);
      assertThat(response.getStatus()).isEqualTo(InvitationStatus.PENDING);
      verify(invitationRepository).save(any(TeamInvitation.class));
      verify(notificationService).notifyRecruitRequest(2L, "팀이름", "리더", 1L);
    }

    @Test
    @DisplayName("팀 리더가 아닌 사용자는 영입 요청을 보낼 수 없다")
    void sendInvitationFailsWhenNotLeader() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User otherUser = createCertifiedUser(3L, "other@example.com", "다른사람");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamInvitationRequest request = createInvitationRequest(2L, JobRole.BACKEND, "함께해요!", 85);

      given(userRepository.findById(3L)).willReturn(Optional.of(otherUser));
      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(userRepository.findById(2L)).willReturn(Optional.of(invitedUser));

      // when & then
      assertThatThrownBy(() -> invitationService.sendInvitation(3L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.NOT_TEAM_LEADER);

      verify(invitationRepository, never()).save(any(TeamInvitation.class));
    }

    @Test
    @DisplayName("자기 자신에게 영입 요청을 보낼 수 없다")
    void sendInvitationFailsWhenSelf() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamInvitationRequest request = createInvitationRequest(1L, JobRole.BACKEND, "함께해요!", 85);

      given(userRepository.findById(1L)).willReturn(Optional.of(leader));
      given(teamRepository.findById(1L)).willReturn(Optional.of(team));

      // when & then
      assertThatThrownBy(() -> invitationService.sendInvitation(1L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.CANNOT_INVITE_SELF);

      verify(invitationRepository, never()).save(any(TeamInvitation.class));
    }

    @Test
    @DisplayName("이미 영입 요청을 보낸 사용자에게 다시 보낼 수 없다")
    void sendInvitationFailsWhenAlreadyInvited() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamInvitationRequest request = createInvitationRequest(2L, JobRole.BACKEND, "함께해요!", 85);

      given(userRepository.findById(1L)).willReturn(Optional.of(leader));
      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(userRepository.findById(2L)).willReturn(Optional.of(invitedUser));
      given(invitationRepository.existsValidPendingInvitation(eq(team), eq(invitedUser), any())).willReturn(true);

      // when & then
      assertThatThrownBy(() -> invitationService.sendInvitation(1L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVITATION_ALREADY_SENT);

      verify(invitationRepository, never()).save(any(TeamInvitation.class));
    }

    @Test
    @DisplayName("이미 지원한 사용자에게 영입 요청을 보낼 수 없다")
    void sendInvitationFailsWhenAlreadyApplied() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamInvitationRequest request = createInvitationRequest(2L, JobRole.BACKEND, "함께해요!", 85);

      given(userRepository.findById(1L)).willReturn(Optional.of(leader));
      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(userRepository.findById(2L)).willReturn(Optional.of(invitedUser));
      given(invitationRepository.existsValidPendingInvitation(eq(team), eq(invitedUser), any())).willReturn(false);
      given(teamMemberRepository.existsByTeamAndUser(team, invitedUser)).willReturn(true);

      // when & then
      assertThatThrownBy(() -> invitationService.sendInvitation(1L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ALREADY_APPLIED);

      verify(invitationRepository, never()).save(any(TeamInvitation.class));
    }

    @Test
    @DisplayName("모집이 완료된 팀에서는 영입 요청을 보낼 수 없다")
    void sendInvitationFailsWhenNotRecruiting() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      team.updateStatus(TeamStatus.COMPLETE);
      TeamInvitationRequest request = createInvitationRequest(2L, JobRole.BACKEND, "함께해요!", 85);

      given(userRepository.findById(1L)).willReturn(Optional.of(leader));
      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(userRepository.findById(2L)).willReturn(Optional.of(invitedUser));

      // when & then
      assertThatThrownBy(() -> invitationService.sendInvitation(1L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.TEAM_NOT_RECRUITING);

      verify(invitationRepository, never()).save(any(TeamInvitation.class));
    }

    @Test
    @DisplayName("모집 중이 아닌 역할로 영입 요청을 보낼 수 없다")
    void sendInvitationFailsWhenRoleNotRecruiting() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      // FRONTEND만 모집 중
      Team team = createTeamWithRecruiting(1L, "팀이름", season, leader, false, false, true, false);
      // BACKEND로 영입 요청
      TeamInvitationRequest request = createInvitationRequest(2L, JobRole.BACKEND, "함께해요!", 85);

      given(userRepository.findById(1L)).willReturn(Optional.of(leader));
      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(userRepository.findById(2L)).willReturn(Optional.of(invitedUser));
      given(invitationRepository.existsValidPendingInvitation(eq(team), eq(invitedUser), any())).willReturn(false);
      given(teamMemberRepository.existsByTeamAndUser(team, invitedUser)).willReturn(false);
      given(teamRepository.findByLeader(invitedUser)).willReturn(List.of());
      given(teamMemberRepository.existsAcceptedMemberInSeason(invitedUser, season)).willReturn(false);

      // when & then
      assertThatThrownBy(() -> invitationService.sendInvitation(1L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

      verify(invitationRepository, never()).save(any(TeamInvitation.class));
    }

    @Test
    @DisplayName("이미 다른 팀에 소속된 사용자에게 영입 요청을 보낼 수 없다")
    void sendInvitationFailsWhenAlreadyInTeam() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeamWithRecruiting(1L, "팀이름", season, leader, true, false, false, true);
      TeamInvitationRequest request = createInvitationRequest(2L, JobRole.BACKEND, "함께해요!", 85);

      given(userRepository.findById(1L)).willReturn(Optional.of(leader));
      given(teamRepository.findById(1L)).willReturn(Optional.of(team));
      given(userRepository.findById(2L)).willReturn(Optional.of(invitedUser));
      given(invitationRepository.existsValidPendingInvitation(eq(team), eq(invitedUser), any())).willReturn(false);
      given(teamMemberRepository.existsByTeamAndUser(team, invitedUser)).willReturn(false);
      given(teamRepository.findByLeader(invitedUser)).willReturn(List.of());
      given(teamMemberRepository.existsAcceptedMemberInSeason(invitedUser, season)).willReturn(true);

      // when & then
      assertThatThrownBy(() -> invitationService.sendInvitation(1L, 1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ALREADY_IN_TEAM);

      verify(invitationRepository, never()).save(any(TeamInvitation.class));
    }
  }

  @Nested
  @DisplayName("영입 요청 수락")
  class AcceptInvitation {

    @Test
    @DisplayName("초대받은 사용자가 영입 요청을 수락할 수 있다")
    void acceptInvitationSuccess() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeamWithRecruiting(1L, "팀이름", season, leader, true, false, false, true);
      TeamInvitation invitation = createInvitation(1L, team, invitedUser, leader, JobRole.BACKEND);

      given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));
      given(teamRepository.findByLeader(invitedUser)).willReturn(List.of());
      given(teamMemberRepository.existsAcceptedMemberInSeason(invitedUser, season)).willReturn(false);
      given(teamMemberRepository.existsByTeamAndUser(team, invitedUser)).willReturn(false);
      given(teamMemberRepository.save(any(TeamMember.class))).willAnswer(invocation -> {
        TeamMember member = invocation.getArgument(0);
        setField(member, "id", 1L);
        return member;
      });

      // when
      TeamInvitationResponse response = invitationService.acceptInvitation(2L, 1L);

      // then
      assertThat(response).isNotNull();
      assertThat(response.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
      verify(teamMemberRepository).save(any(TeamMember.class));
      verify(notificationService).notifyRecruitAccepted(1L, "초대대상", "팀이름", 1L);
    }

    @Test
    @DisplayName("초대받지 않은 사용자는 영입 요청을 수락할 수 없다")
    void acceptInvitationFailsWhenNotInvitedUser() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      User otherUser = createCertifiedUser(3L, "other@example.com", "다른사람");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamInvitation invitation = createInvitation(1L, team, invitedUser, leader, JobRole.BACKEND);

      given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));

      // when & then
      assertThatThrownBy(() -> invitationService.acceptInvitation(3L, 1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ACCESS_DENIED);

      verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("이미 처리된 영입 요청은 수락할 수 없다")
    void acceptInvitationFailsWhenNotPending() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamInvitation invitation = createInvitation(1L, team, invitedUser, leader, JobRole.BACKEND);
      invitation.accept(); // 이미 수락됨

      given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));

      // when & then
      assertThatThrownBy(() -> invitationService.acceptInvitation(2L, 1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVITATION_NOT_PENDING);

      verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("만료된 영입 요청은 수락할 수 없다")
    void acceptInvitationFailsWhenExpired() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamInvitation invitation = createInvitation(1L, team, invitedUser, leader, JobRole.BACKEND);
      // 만료일을 과거로 설정
      setField(invitation, "expiresAt", LocalDateTime.now().minusDays(1));

      given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));

      // when & then
      assertThatThrownBy(() -> invitationService.acceptInvitation(2L, 1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVITATION_EXPIRED);

      verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("팀 모집이 완료된 후에는 영입 요청을 수락할 수 없다")
    void acceptInvitationFailsWhenTeamNotRecruiting() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      team.updateStatus(TeamStatus.COMPLETE); // 모집 완료
      TeamInvitation invitation = createInvitation(1L, team, invitedUser, leader, JobRole.BACKEND);

      given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));

      // when & then
      assertThatThrownBy(() -> invitationService.acceptInvitation(2L, 1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.TEAM_NOT_RECRUITING);

      verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("해당 역할이 더 이상 모집 중이 아니면 영입 요청을 수락할 수 없다")
    void acceptInvitationFailsWhenRoleNotRecruiting() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      // BACKEND 모집 중이었지만 이제 아님
      Team team = createTeamWithRecruiting(1L, "팀이름", season, leader, false, false, true, false);
      TeamInvitation invitation = createInvitation(1L, team, invitedUser, leader, JobRole.BACKEND);

      given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));

      // when & then
      assertThatThrownBy(() -> invitationService.acceptInvitation(2L, 1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

      verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("이미 팀에 지원 기록이 있으면 영입 요청을 수락할 수 없다")
    void acceptInvitationFailsWhenAlreadyApplied() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeamWithRecruiting(1L, "팀이름", season, leader, true, false, false, true);
      TeamInvitation invitation = createInvitation(1L, team, invitedUser, leader, JobRole.BACKEND);

      given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));
      // validateNotAlreadyApplied에서 실패하므로 validateNotAlreadyInTeam 관련 mock은 불필요
      given(teamMemberRepository.existsByTeamAndUser(team, invitedUser)).willReturn(true);

      // when & then
      assertThatThrownBy(() -> invitationService.acceptInvitation(2L, 1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ALREADY_APPLIED);

      verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }
  }

  @Nested
  @DisplayName("영입 요청 거절")
  class RejectInvitation {

    @Test
    @DisplayName("초대받은 사용자가 영입 요청을 거절할 수 있다")
    void rejectInvitationSuccess() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamInvitation invitation = createInvitation(1L, team, invitedUser, leader, JobRole.BACKEND);

      given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));

      // when
      TeamInvitationResponse response = invitationService.rejectInvitation(2L, 1L);

      // then
      assertThat(response).isNotNull();
      assertThat(response.getStatus()).isEqualTo(InvitationStatus.REJECTED);
      verify(notificationService).notifyRecruitRejected(1L, "초대대상", "팀이름");
    }

    @Test
    @DisplayName("초대받지 않은 사용자는 영입 요청을 거절할 수 없다")
    void rejectInvitationFailsWhenNotInvitedUser() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      User otherUser = createCertifiedUser(3L, "other@example.com", "다른사람");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamInvitation invitation = createInvitation(1L, team, invitedUser, leader, JobRole.BACKEND);

      given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));

      // when & then
      assertThatThrownBy(() -> invitationService.rejectInvitation(3L, 1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ACCESS_DENIED);
    }
  }

  @Nested
  @DisplayName("영입 요청 취소")
  class CancelInvitation {

    @Test
    @DisplayName("팀 리더가 영입 요청을 취소할 수 있다")
    void cancelInvitationSuccess() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamInvitation invitation = createInvitation(1L, team, invitedUser, leader, JobRole.BACKEND);

      given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));

      // when
      invitationService.cancelInvitation(1L, 1L);

      // then
      verify(invitationRepository).delete(invitation);
    }

    @Test
    @DisplayName("팀 리더가 아닌 사용자는 영입 요청을 취소할 수 없다")
    void cancelInvitationFailsWhenNotLeader() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamInvitation invitation = createInvitation(1L, team, invitedUser, leader, JobRole.BACKEND);

      given(invitationRepository.findById(1L)).willReturn(Optional.of(invitation));

      // when & then
      assertThatThrownBy(() -> invitationService.cancelInvitation(3L, 1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.NOT_TEAM_LEADER);

      verify(invitationRepository, never()).delete(any(TeamInvitation.class));
    }
  }

  @Nested
  @DisplayName("내가 받은 영입 요청 조회")
  class GetMyInvitations {

    @Test
    @DisplayName("내가 받은 영입 요청 목록을 조회할 수 있다")
    void getMyInvitationsSuccess() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamInvitation invitation = createInvitation(1L, team, invitedUser, leader, JobRole.BACKEND);

      given(userRepository.findById(2L)).willReturn(Optional.of(invitedUser));
      given(invitationRepository.findByInvitedUserOrderByCreatedAtDesc(invitedUser))
          .willReturn(List.of(invitation));

      // when
      List<TeamInvitationResponse> responses = invitationService.getMyInvitations(2L);

      // then
      assertThat(responses).hasSize(1);
      assertThat(responses.get(0).getSuggestedRole()).isEqualTo(JobRole.BACKEND);
    }
  }

  @Nested
  @DisplayName("만료된 영입 요청 처리")
  class ExpireInvitations {

    @Test
    @DisplayName("만료된 영입 요청을 EXPIRED 상태로 변경한다")
    void expireOldInvitationsSuccess() {
      // given
      User leader = createCertifiedUser(1L, "leader@example.com", "리더");
      User invitedUser = createCertifiedUser(2L, "invited@example.com", "초대대상");
      Season season = createSeason(1L, "시즌1", SeasonStatus.TEAM_BUILDING);
      Team team = createTeam(1L, "팀이름", season, leader);
      TeamInvitation invitation = createInvitation(1L, team, invitedUser, leader, JobRole.BACKEND);
      setField(invitation, "expiresAt", LocalDateTime.now().minusDays(1));

      given(invitationRepository.findExpiredInvitations(any())).willReturn(List.of(invitation));

      // when
      int count = invitationService.expireOldInvitations();

      // then
      assertThat(count).isEqualTo(1);
      assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
    }
  }

  // 헬퍼 메서드
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

  private TeamInvitation createInvitation(Long id, Team team, User invitedUser, User invitedBy, JobRole role) {
    TeamInvitation invitation = TeamInvitation.builder()
        .team(team)
        .invitedUser(invitedUser)
        .invitedBy(invitedBy)
        .suggestedRole(role)
        .message("함께해요!")
        .matchScore(85)
        .build();
    setField(invitation, "id", id);
    return invitation;
  }

  private TeamInvitationRequest createInvitationRequest(Long userId, JobRole role, String message, Integer matchScore) {
    TeamInvitationRequest request = new TeamInvitationRequest();
    setField(request, "userId", userId);
    setField(request, "role", role);
    setField(request, "message", message);
    setField(request, "matchScore", matchScore);
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
