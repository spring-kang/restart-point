package com.restartpoint.domain.team.service;

import com.restartpoint.domain.notification.service.NotificationService;
import com.restartpoint.domain.profile.entity.JobRole;
import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.team.dto.TeamInvitationRequest;
import com.restartpoint.domain.team.dto.TeamInvitationResponse;
import com.restartpoint.domain.team.entity.*;
import com.restartpoint.domain.team.repository.TeamInvitationRepository;
import com.restartpoint.domain.team.repository.TeamMemberRepository;
import com.restartpoint.domain.team.repository.TeamRepository;
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TeamInvitationService {

    private final TeamInvitationRepository invitationRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 영입 요청 발송 (팀 리더만 가능)
     */
    @Transactional
    public TeamInvitationResponse sendInvitation(Long leaderId, Long teamId, TeamInvitationRequest request) {
        User leader = findUserById(leaderId);
        Team team = findTeamById(teamId);
        User invitedUser = findUserById(request.getUserId());

        // 검증
        validateTeamLeader(team, leaderId);
        validateTeamRecruiting(team);
        validateNotSelf(leaderId, request.getUserId());
        validateNotAlreadyInvited(team, invitedUser);
        validateNotAlreadyApplied(team, invitedUser);  // 이미 지원한 사용자인지 확인
        validateNotAlreadyInTeam(invitedUser, team.getSeason());
        validateTeamNotFull(team);
        validateRecruitingRole(team, request.getRole());

        // 영입 요청 생성
        TeamInvitation invitation = TeamInvitation.builder()
                .team(team)
                .invitedUser(invitedUser)
                .invitedBy(leader)
                .suggestedRole(request.getRole())
                .message(request.getMessage())
                .matchScore(request.getMatchScore())
                .build();

        TeamInvitation saved = invitationRepository.save(invitation);
        log.info("영입 요청 발송: teamId={}, invitedUserId={}, role={}",
                teamId, request.getUserId(), request.getRole());

        // 알림 발송
        notificationService.notifyRecruitRequest(
                invitedUser.getId(),
                team.getName(),
                leader.getName(),
                team.getId()
        );

        return TeamInvitationResponse.from(saved);
    }

    /**
     * 내가 받은 영입 요청 목록 조회
     */
    public List<TeamInvitationResponse> getMyInvitations(Long userId) {
        User user = findUserById(userId);
        return invitationRepository.findByInvitedUserOrderByCreatedAtDesc(user).stream()
                .map(TeamInvitationResponse::from)
                .toList();
    }

    /**
     * 내가 받은 대기 중인 영입 요청 목록 조회
     */
    public List<TeamInvitationResponse> getMyPendingInvitations(Long userId) {
        User user = findUserById(userId);
        return invitationRepository.findByInvitedUserAndStatus(user, InvitationStatus.PENDING).stream()
                .filter(inv -> !inv.isExpired())
                .map(TeamInvitationResponse::from)
                .toList();
    }

    /**
     * 대기 중인 영입 요청 수 조회
     */
    public long getPendingInvitationCount(Long userId) {
        User user = findUserById(userId);
        return invitationRepository.findByInvitedUserAndStatus(user, InvitationStatus.PENDING).stream()
                .filter(inv -> !inv.isExpired())
                .count();
    }

    /**
     * 팀에서 보낸 영입 요청 목록 조회 (팀 리더만 가능)
     */
    public List<TeamInvitationResponse> getTeamInvitations(Long leaderId, Long teamId) {
        Team team = findTeamById(teamId);
        validateTeamLeader(team, leaderId);

        return invitationRepository.findByTeamOrderByCreatedAtDesc(team).stream()
                .map(TeamInvitationResponse::from)
                .toList();
    }

    /**
     * 영입 요청 수락
     */
    @Transactional
    public TeamInvitationResponse acceptInvitation(Long userId, Long invitationId) {
        TeamInvitation invitation = findInvitationById(invitationId);
        Team team = invitation.getTeam();
        User invitedUser = invitation.getInvitedUser();

        // 검증
        validateInvitedUser(invitation, userId);
        validateInvitationPending(invitation);
        validateInvitationNotExpired(invitation);
        validateCertificationRequirement(team.getSeason(), invitedUser);  // 시즌별 인증 요구사항 확인
        validateTeamRecruiting(team);  // 팀이 아직 모집 중인지 확인
        validateRecruitingRole(team, invitation.getSuggestedRole());  // 역할이 아직 열려있는지 확인
        validateTeamNotFull(team);
        validateNotAlreadyApplied(team, invitedUser);  // 중복 TeamMember 방지
        validateNotAlreadyInTeam(invitedUser, team.getSeason());

        // 영입 요청 수락
        invitation.accept();

        // 팀원으로 추가
        TeamMember teamMember = TeamMember.builder()
                .team(team)
                .user(invitation.getInvitedUser())
                .role(invitation.getSuggestedRole())
                .applicationMessage("영입 요청을 통해 합류")
                .build();
        teamMember.accept();
        teamMemberRepository.save(teamMember);
        team.addMember(teamMember);

        log.info("영입 요청 수락: invitationId={}, userId={}, teamId={}",
                invitationId, userId, invitation.getTeam().getId());

        // 팀 리더에게 알림 발송
        notificationService.notifyRecruitAccepted(
                invitation.getInvitedBy().getId(),
                invitation.getInvitedUser().getName(),
                invitation.getTeam().getName(),
                invitation.getTeam().getId()
        );

        return TeamInvitationResponse.from(invitation);
    }

    /**
     * 영입 요청 거절
     */
    @Transactional
    public TeamInvitationResponse rejectInvitation(Long userId, Long invitationId) {
        TeamInvitation invitation = findInvitationById(invitationId);

        // 검증
        validateInvitedUser(invitation, userId);
        validateInvitationPending(invitation);

        // 영입 요청 거절
        invitation.reject();

        log.info("영입 요청 거절: invitationId={}, userId={}", invitationId, userId);

        // 팀 리더에게 알림 발송
        notificationService.notifyRecruitRejected(
                invitation.getInvitedBy().getId(),
                invitation.getInvitedUser().getName(),
                invitation.getTeam().getName()
        );

        return TeamInvitationResponse.from(invitation);
    }

    /**
     * 영입 요청 취소 (팀 리더만 가능)
     */
    @Transactional
    public void cancelInvitation(Long leaderId, Long invitationId) {
        TeamInvitation invitation = findInvitationById(invitationId);

        // 검증
        validateTeamLeader(invitation.getTeam(), leaderId);
        validateInvitationPending(invitation);

        invitationRepository.delete(invitation);
        log.info("영입 요청 취소: invitationId={}, leaderId={}", invitationId, leaderId);
    }

    /**
     * 만료된 영입 요청 처리 (스케줄러에서 호출)
     */
    @Transactional
    public int expireOldInvitations() {
        List<TeamInvitation> expiredInvitations =
                invitationRepository.findExpiredInvitations(LocalDateTime.now());

        for (TeamInvitation invitation : expiredInvitations) {
            invitation.expire();
        }

        log.info("만료된 영입 요청 처리: count={}", expiredInvitations.size());
        return expiredInvitations.size();
    }

    // ========== Private Helper Methods ==========

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Team findTeamById(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    private TeamInvitation findInvitationById(Long invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));
    }

    private void validateTeamLeader(Team team, Long userId) {
        if (!team.getLeader().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_TEAM_LEADER);
        }
    }

    private void validateTeamRecruiting(Team team) {
        if (team.getStatus() != TeamStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.TEAM_NOT_RECRUITING);
        }
    }

    private void validateNotSelf(Long leaderId, Long invitedUserId) {
        if (leaderId.equals(invitedUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_INVITE_SELF);
        }
    }

    private void validateNotAlreadyInvited(Team team, User invitedUser) {
        // 만료되지 않은 PENDING 상태의 영입 요청이 있는지 확인
        if (invitationRepository.existsValidPendingInvitation(
                team, invitedUser, LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVITATION_ALREADY_SENT);
        }
    }

    private void validateNotAlreadyApplied(Team team, User user) {
        // 이미 해당 팀에 지원한 레코드가 있는지 확인
        if (teamMemberRepository.existsByTeamAndUser(team, user)) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED, "해당 사용자는 이미 이 팀에 지원했습니다.");
        }
    }

    private void validateNotAlreadyInTeam(User user, Season season) {
        // 리더로서 팀이 있는지 확인
        boolean isLeader = teamRepository.findByLeader(user).stream()
                .anyMatch(t -> t.getSeason().getId().equals(season.getId()));
        if (isLeader) {
            throw new BusinessException(ErrorCode.ALREADY_IN_TEAM);
        }

        // 팀원으로서 ACCEPTED 상태인 팀이 있는지 확인
        if (teamMemberRepository.existsAcceptedMemberInSeason(user, season)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_TEAM);
        }
    }

    private void validateTeamNotFull(Team team) {
        if (team.isFull()) {
            throw new BusinessException(ErrorCode.TEAM_FULL);
        }
    }

    private void validateRecruitingRole(Team team, JobRole role) {
        boolean isRecruiting = switch (role) {
            case PLANNER -> Boolean.TRUE.equals(team.getRecruitingPlanner());
            case UXUI -> Boolean.TRUE.equals(team.getRecruitingUxui());
            case FRONTEND -> Boolean.TRUE.equals(team.getRecruitingFrontend());
            case BACKEND -> Boolean.TRUE.equals(team.getRecruitingBackend());
        };

        if (!isRecruiting) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "현재 모집 중인 역할이 아닙니다.");
        }
    }

    private void validateInvitedUser(TeamInvitation invitation, Long userId) {
        if (!invitation.getInvitedUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateInvitationPending(TeamInvitation invitation) {
        if (!invitation.isPending()) {
            throw new BusinessException(ErrorCode.INVITATION_NOT_PENDING);
        }
    }

    private void validateInvitationNotExpired(TeamInvitation invitation) {
        if (invitation.isExpired()) {
            throw new BusinessException(ErrorCode.INVITATION_EXPIRED);
        }
    }

    private void validateCertificationRequirement(Season season, User user) {
        if (!season.canUserParticipate(user.isCertified())) {
            throw new BusinessException(ErrorCode.CERTIFICATION_REQUIRED);
        }
    }
}
