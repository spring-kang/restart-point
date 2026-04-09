package com.restartpoint.domain.team.repository;

import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.entity.TeamMember;
import com.restartpoint.domain.team.entity.TeamMemberStatus;
import com.restartpoint.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByTeamId(Long teamId);

    List<TeamMember> findByTeam(Team team);

    List<TeamMember> findByTeamAndStatus(Team team, TeamMemberStatus status);

    List<TeamMember> findByUser(User user);

    Optional<TeamMember> findByTeamAndUser(Team team, User user);

    boolean existsByTeamAndUser(Team team, User user);

    boolean existsByTeamAndUserAndStatus(Team team, User user, TeamMemberStatus status);

    // 특정 시즌에서 사용자가 이미 ACCEPTED 상태인 팀이 있는지 확인
    @Query("SELECT COUNT(tm) > 0 FROM TeamMember tm WHERE tm.user = :user AND tm.status = 'ACCEPTED' AND tm.team.season = :season")
    boolean existsAcceptedMemberInSeason(@Param("user") User user, @Param("season") Season season);

    // 특정 시즌에서 사용자가 리더인 팀이 있는지 확인
    @Query("SELECT COUNT(t) > 0 FROM Team t WHERE t.leader = :user AND t.season = :season")
    boolean existsLeaderInSeason(@Param("user") User user, @Param("season") Season season);
}
