package com.restartpoint.domain.team.repository;

import com.restartpoint.domain.team.entity.InvitationStatus;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.entity.TeamInvitation;
import com.restartpoint.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {

    // 특정 사용자가 받은 영입 요청 목록 (PENDING 상태)
    List<TeamInvitation> findByInvitedUserAndStatus(User invitedUser, InvitationStatus status);

    // 특정 사용자가 받은 모든 영입 요청 목록
    List<TeamInvitation> findByInvitedUserOrderByCreatedAtDesc(User invitedUser);

    // 특정 팀에서 보낸 영입 요청 목록
    List<TeamInvitation> findByTeamOrderByCreatedAtDesc(Team team);

    // 특정 팀과 사용자에 대한 PENDING 상태의 영입 요청이 있는지 확인
    boolean existsByTeamAndInvitedUserAndStatus(Team team, User invitedUser, InvitationStatus status);

    // 특정 팀과 사용자에 대한 아직 만료되지 않은 PENDING 상태의 영입 요청이 있는지 확인
    @Query("SELECT COUNT(ti) > 0 FROM TeamInvitation ti " +
           "WHERE ti.team = :team AND ti.invitedUser = :invitedUser " +
           "AND ti.status = 'PENDING' AND ti.expiresAt > :now")
    boolean existsValidPendingInvitation(
            @Param("team") Team team,
            @Param("invitedUser") User invitedUser,
            @Param("now") LocalDateTime now);

    // 특정 팀과 사용자에 대한 영입 요청 조회
    Optional<TeamInvitation> findByTeamAndInvitedUser(Team team, User invitedUser);

    // 만료된 영입 요청 조회 (배치 처리용)
    @Query("SELECT ti FROM TeamInvitation ti WHERE ti.status = 'PENDING' AND ti.expiresAt < :now")
    List<TeamInvitation> findExpiredInvitations(@Param("now") LocalDateTime now);

    // 특정 사용자가 받은 PENDING 상태 영입 요청 수
    long countByInvitedUserAndStatus(User invitedUser, InvitationStatus status);
}
