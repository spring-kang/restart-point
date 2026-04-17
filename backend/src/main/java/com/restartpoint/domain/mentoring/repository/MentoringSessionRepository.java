package com.restartpoint.domain.mentoring.repository;

import com.restartpoint.domain.mentoring.entity.MentoringSession;
import com.restartpoint.domain.mentoring.entity.MentoringSession.SessionStatus;
import com.restartpoint.domain.profile.entity.JobRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MentoringSessionRepository extends JpaRepository<MentoringSession, Long> {

    List<MentoringSession> findByMenteeId(Long menteeId);

    List<MentoringSession> findByMentorId(Long mentorId);

    Optional<MentoringSession> findByMenteeIdAndModuleId(Long menteeId, Long moduleId);

    @Query("SELECT ms FROM MentoringSession ms " +
           "JOIN ms.mentee tm " +
           "JOIN tm.user u " +
           "WHERE u.id = :userId " +
           "ORDER BY ms.createdAt DESC")
    List<MentoringSession> findByUserId(@Param("userId") Long userId);

    @Query("SELECT ms FROM MentoringSession ms " +
           "JOIN ms.mentee tm " +
           "JOIN tm.team t " +
           "WHERE t.season.id = :seasonId AND tm.user.id = :userId " +
           "ORDER BY ms.module.weekNumber ASC")
    List<MentoringSession> findByUserIdAndSeasonId(
            @Param("userId") Long userId,
            @Param("seasonId") Long seasonId);

    @Query("SELECT ms FROM MentoringSession ms " +
           "JOIN ms.module mm " +
           "JOIN mm.mentoring jrm " +
           "WHERE jrm.season.id = :seasonId AND jrm.jobRole = :jobRole " +
           "AND ms.status = :status")
    List<MentoringSession> findBySeasonIdAndJobRoleAndStatus(
            @Param("seasonId") Long seasonId,
            @Param("jobRole") JobRole jobRole,
            @Param("status") SessionStatus status);

    @Query("SELECT COUNT(ms) FROM MentoringSession ms " +
           "JOIN ms.mentee tm " +
           "WHERE tm.user.id = :userId AND ms.status = 'COMPLETED'")
    long countCompletedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(ms) FROM MentoringSession ms " +
           "JOIN ms.mentee tm " +
           "JOIN tm.team t " +
           "WHERE t.season.id = :seasonId AND tm.user.id = :userId AND ms.status = 'COMPLETED'")
    long countCompletedByUserIdAndSeasonId(
            @Param("userId") Long userId,
            @Param("seasonId") Long seasonId);

    @Query("SELECT AVG(ms.feedbackScore) FROM MentoringSession ms " +
           "JOIN ms.mentee tm " +
           "WHERE tm.user.id = :userId AND ms.feedbackScore IS NOT NULL")
    Optional<Double> findAverageFeedbackScoreByUserId(@Param("userId") Long userId);

    List<MentoringSession> findByStatusAndMentorIsNull(SessionStatus status);

    boolean existsByMenteeIdAndModuleId(Long menteeId, Long moduleId);
}
