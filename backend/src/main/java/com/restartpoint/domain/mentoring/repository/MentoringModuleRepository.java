package com.restartpoint.domain.mentoring.repository;

import com.restartpoint.domain.mentoring.entity.MentoringModule;
import com.restartpoint.domain.profile.entity.JobRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MentoringModuleRepository extends JpaRepository<MentoringModule, Long> {

    List<MentoringModule> findByMentoringIdOrderByWeekNumberAsc(Long mentoringId);

    Optional<MentoringModule> findByMentoringIdAndWeekNumber(Long mentoringId, Integer weekNumber);

    @Query("SELECT mm FROM MentoringModule mm " +
           "JOIN mm.mentoring jrm " +
           "WHERE jrm.season.id = :seasonId AND jrm.jobRole = :jobRole AND jrm.active = true " +
           "ORDER BY mm.weekNumber ASC")
    List<MentoringModule> findBySeasonIdAndJobRole(
            @Param("seasonId") Long seasonId,
            @Param("jobRole") JobRole jobRole);

    @Query("SELECT mm FROM MentoringModule mm " +
           "JOIN mm.mentoring jrm " +
           "WHERE jrm.season.id = :seasonId AND jrm.jobRole = :jobRole " +
           "AND mm.weekNumber = :weekNumber AND jrm.active = true")
    Optional<MentoringModule> findBySeasonIdAndJobRoleAndWeekNumber(
            @Param("seasonId") Long seasonId,
            @Param("jobRole") JobRole jobRole,
            @Param("weekNumber") Integer weekNumber);

    boolean existsByMentoringIdAndWeekNumber(Long mentoringId, Integer weekNumber);

    @Query("SELECT MAX(mm.weekNumber) FROM MentoringModule mm WHERE mm.mentoring.id = :mentoringId")
    Optional<Integer> findMaxWeekNumberByMentoringId(@Param("mentoringId") Long mentoringId);
}
