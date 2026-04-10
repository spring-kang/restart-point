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
import com.restartpoint.domain.user.entity.User;
import com.restartpoint.domain.user.repository.UserRepository;
import com.restartpoint.global.exception.BusinessException;
import com.restartpoint.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SeasonRepository seasonRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // 팀 생성
    @Transactional
    public TeamResponse createTeam(Long userId, TeamRequest request) {
        User user = findUserById(userId);
        Season season = findSeasonById(request.getSeasonId());

        // 수료 인증 확인
        if (!user.isCertified()) {
            throw new BusinessException(ErrorCode.CERTIFICATION_REQUIRED);
        }

        // 팀빌딩 기간인지 확인
        if (season.getStatus() != SeasonStatus.TEAM_BUILDING) {
            throw new BusinessException(ErrorCode.SEASON_NOT_TEAM_BUILDING);
        }

        // 이미 해당 시즌에서 팀에 소속되어 있는지 확인
        if (isUserInTeamForSeason(user, season)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_TEAM);
        }

        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .season(season)
                .leader(user)
                .recruitingPlanner(request.getRecruitingPlanner())
                .recruitingUxui(request.getRecruitingUxui())
                .recruitingFrontend(request.getRecruitingFrontend())
                .recruitingBackend(request.getRecruitingBackend())
                .build();

        Team savedTeam = teamRepository.save(team);

        // 리더를 팀원으로 추가 (ACCEPTED 상태)
        TeamMember leaderMember = TeamMember.builder()
                .team(savedTeam)
                .user(user)
                .role(request.getLeaderRole())
                .build();
        leaderMember.accept();
        teamMemberRepository.save(leaderMember);
        savedTeam.addMember(leaderMember);

        return TeamResponse.from(savedTeam);
    }

    // 팀 상세 조회
    public TeamResponse getTeam(Long teamId) {
        Team team = findTeamById(teamId);
        return TeamResponse.from(team);
    }

    // 시즌별 팀 목록 조회
    public List<TeamResponse> getTeamsBySeason(Long seasonId) {
        Season season = findSeasonById(seasonId);
        return teamRepository.findBySeason(season).stream()
                .map(TeamResponse::simpleFrom)
                .toList();
    }

    // 시즌별 모집 중인 팀 목록 조회
    public List<TeamResponse> getRecruitingTeamsBySeason(Long seasonId) {
        Season season = findSeasonById(seasonId);
        return teamRepository.findRecruitingTeamsBySeason(season).stream()
                .map(TeamResponse::simpleFrom)
                .toList();
    }

    // 내가 리더인 팀 목록 조회
    public List<TeamResponse> getMyTeams(Long userId) {
        User user = findUserById(userId);
        return teamRepository.findByLeader(user).stream()
                .map(TeamResponse::simpleFrom)
                .toList();
    }

    // 내가 멤버로 속한 팀 목록 조회 (리더인 팀 제외)
    public List<TeamResponse> getTeamsAsMember(Long userId) {
        User user = findUserById(userId);
        return teamRepository.findTeamsByMember(user).stream()
                .filter(team -> !team.getLeader().getId().equals(userId))
                .map(TeamResponse::simpleFrom)
                .toList();
    }

    // 팀 정보 수정 (리더만 가능)
    @Transactional
    public TeamResponse updateTeam(Long userId, Long teamId, TeamRequest request) {
        Team team = findTeamById(teamId);
        validateTeamLeader(team, userId);

        // 팀 정보 업데이트 메서드 추가 필요
        team.updateRecruitingRoles(
                request.getRecruitingPlanner(),
                request.getRecruitingUxui(),
                request.getRecruitingFrontend(),
                request.getRecruitingBackend()
        );

        return TeamResponse.from(team);
    }

    // 팀 상태 변경 (리더만 가능)
    @Transactional
    public TeamResponse updateTeamStatus(Long userId, Long teamId, TeamStatus status) {
        Team team = findTeamById(teamId);
        validateTeamLeader(team, userId);
        team.updateStatus(status);
        return TeamResponse.from(team);
    }

    // 팀 지원
    @Transactional
    public TeamMemberResponse applyToTeam(Long userId, Long teamId, TeamApplyRequest request) {
        User user = findUserById(userId);
        Team team = findTeamById(teamId);

        // 수료 인증 확인
        if (!user.isCertified()) {
            throw new BusinessException(ErrorCode.CERTIFICATION_REQUIRED);
        }

        // 팀이 모집 중인지 확인
        if (team.getStatus() != TeamStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.SEASON_NOT_RECRUITING);
        }

        // 팀이 가득 찼는지 확인
        if (team.isFull()) {
            throw new BusinessException(ErrorCode.TEAM_FULL);
        }

        // 이미 해당 시즌에서 팀에 소속되어 있는지 확인
        if (isUserInTeamForSeason(user, team.getSeason())) {
            throw new BusinessException(ErrorCode.ALREADY_IN_TEAM);
        }

        // 이미 지원했는지 확인
        if (teamMemberRepository.existsByTeamAndUser(team, user)) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }

        // 지원 역할이 모집 중인지 확인
        validateRecruitingRole(team, request.getRole());

        TeamMember teamMember = TeamMember.builder()
                .team(team)
                .user(user)
                .role(request.getRole())
                .applicationMessage(request.getApplicationMessage())
                .build();

        TeamMember savedMember = teamMemberRepository.save(teamMember);
        team.addMember(savedMember);

        // 팀 리더에게 지원 알림 발송
        notificationService.notifyTeamApplication(
                team.getLeader().getId(),
                user.getName(),
                team.getName(),
                team.getId()
        );

        return TeamMemberResponse.from(savedMember);
    }

    // 팀 지원 목록 조회 (리더만 가능)
    public List<TeamMemberResponse> getTeamApplications(Long userId, Long teamId) {
        Team team = findTeamById(teamId);
        validateTeamLeader(team, userId);

        return teamMemberRepository.findByTeamAndStatus(team, TeamMemberStatus.PENDING).stream()
                .map(TeamMemberResponse::from)
                .toList();
    }

    // 팀 멤버 목록 조회
    public List<TeamMemberResponse> getTeamMembers(Long teamId) {
        Team team = findTeamById(teamId);
        return teamMemberRepository.findByTeamAndStatus(team, TeamMemberStatus.ACCEPTED).stream()
                .map(TeamMemberResponse::from)
                .toList();
    }

    // 팀 지원 수락 (리더만 가능)
    @Transactional
    public TeamMemberResponse acceptApplication(Long userId, Long teamId, Long memberId) {
        Team team = findTeamById(teamId);
        validateTeamLeader(team, userId);

        TeamMember member = findTeamMemberById(memberId);
        validateMemberBelongsToTeam(member, team);

        if (!member.isPending()) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }

        if (team.isFull()) {
            throw new BusinessException(ErrorCode.TEAM_FULL);
        }

        // 지원자가 이미 다른 팀에 소속되어 있는지 확인
        if (isUserInTeamForSeason(member.getUser(), team.getSeason())) {
            throw new BusinessException(ErrorCode.ALREADY_IN_TEAM, "해당 사용자는 이미 다른 팀에 소속되어 있습니다.");
        }

        member.accept();

        // 지원자에게 가입 승인 알림 발송
        notificationService.notifyTeamInvitation(
                member.getUser().getId(),
                team.getName(),
                team.getId()
        );

        return TeamMemberResponse.from(member);
    }

    // 팀 지원 거절 (리더만 가능)
    @Transactional
    public TeamMemberResponse rejectApplication(Long userId, Long teamId, Long memberId) {
        Team team = findTeamById(teamId);
        validateTeamLeader(team, userId);

        TeamMember member = findTeamMemberById(memberId);
        validateMemberBelongsToTeam(member, team);

        if (!member.isPending()) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }

        member.reject();

        // 지원자에게 거절 알림 발송
        notificationService.notifyTeamApplicationRejected(
                member.getUser().getId(),
                team.getName()
        );

        return TeamMemberResponse.from(member);
    }

    // 팀 탈퇴
    @Transactional
    public void leaveTeam(Long userId, Long teamId) {
        Team team = findTeamById(teamId);
        User user = findUserById(userId);

        // 리더는 탈퇴 불가
        if (team.getLeader().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_TEAM_LEADER, "팀 리더는 탈퇴할 수 없습니다. 팀을 해체하거나 리더를 위임하세요.");
        }

        TeamMember member = teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));

        team.removeMember(member);
        teamMemberRepository.delete(member);
    }

    // 내 지원 현황 조회 (리더로서 자동 생성된 레코드 제외)
    public List<TeamMemberResponse> getMyApplications(Long userId) {
        User user = findUserById(userId);
        return teamMemberRepository.findByUser(user).stream()
                .filter(member -> !member.getTeam().getLeader().getId().equals(userId))
                .map(TeamMemberResponse::from)
                .toList();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Season findSeasonById(Long seasonId) {
        return seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEASON_NOT_FOUND));
    }

    private Team findTeamById(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    private TeamMember findTeamMemberById(Long memberId) {
        return teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    private void validateTeamLeader(Team team, Long userId) {
        if (!team.getLeader().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_TEAM_LEADER);
        }
    }

    private void validateMemberBelongsToTeam(TeamMember member, Team team) {
        if (!member.getTeam().getId().equals(team.getId())) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
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

    // 사용자가 해당 시즌에서 이미 팀에 소속되어 있는지 확인
    private boolean isUserInTeamForSeason(User user, Season season) {
        // 리더로서 팀이 있는지 확인
        boolean isLeader = teamRepository.findByLeader(user).stream()
                .anyMatch(t -> t.getSeason().getId().equals(season.getId()));
        if (isLeader) {
            return true;
        }

        // 팀원으로서 ACCEPTED 상태인 팀이 있는지 확인
        return teamMemberRepository.existsAcceptedMemberInSeason(user, season);
    }
}
