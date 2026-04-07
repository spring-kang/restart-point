package com.restartpoint.domain.team.repository;

import com.restartpoint.domain.season.entity.Season;
import com.restartpoint.domain.team.entity.Team;
import com.restartpoint.domain.team.entity.TeamStatus;
import com.restartpoint.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findBySeason(Season season);

    List<Team> findBySeasonAndStatus(Season season, TeamStatus status);

    List<Team> findByLeader(User leader);

    @Query("SELECT t FROM Team t WHERE t.season = :season AND t.status = 'RECRUITING'")
    List<Team> findRecruitingTeamsBySeason(@Param("season") Season season);

    @Query("SELECT t FROM Team t JOIN t.members m WHERE m.user = :user AND m.status = 'ACCEPTED'")
    List<Team> findTeamsByMember(@Param("user") User user);

    @Query("SELECT COUNT(t) FROM Team t WHERE t.season = :season")
    long countBySeason(@Param("season") Season season);
}
