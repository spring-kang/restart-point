package com.restartpoint.domain.mentoring.repository;

import com.restartpoint.domain.mentoring.entity.JobRoleMentoring;
import com.restartpoint.domain.profile.entity.JobRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobRoleMentoringRepository extends JpaRepository<JobRoleMentoring, Long> {

    List<JobRoleMentoring> findBySeasonId(Long seasonId);

    List<JobRoleMentoring> findBySeasonIdAndActiveTrue(Long seasonId);

    Optional<JobRoleMentoring> findBySeasonIdAndJobRole(Long seasonId, JobRole jobRole);

    Optional<JobRoleMentoring> findBySeasonIdAndJobRoleAndActiveTrue(Long seasonId, JobRole jobRole);

    @Query("SELECT jrm FROM JobRoleMentoring jrm " +
           "LEFT JOIN FETCH jrm.modules " +
           "WHERE jrm.id = :id")
    Optional<JobRoleMentoring> findByIdWithModules(@Param("id") Long id);

    @Query("SELECT jrm FROM JobRoleMentoring jrm " +
           "LEFT JOIN FETCH jrm.modules " +
           "WHERE jrm.season.id = :seasonId AND jrm.jobRole = :jobRole AND jrm.active = true")
    Optional<JobRoleMentoring> findBySeasonIdAndJobRoleWithModules(
            @Param("seasonId") Long seasonId,
            @Param("jobRole") JobRole jobRole);

    boolean existsBySeasonIdAndJobRole(Long seasonId, JobRole jobRole);
}
